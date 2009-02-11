//
// $Id$

package com.threerings.bang.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.StaticConnectionProvider;

import com.threerings.coin.server.persist.CoinRepository;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.GoldPass;
import com.threerings.bang.server.ItemFactory;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PlayerRecord;
import com.threerings.bang.server.persist.PlayerRepository;

/**
 * Handles some migratory bits when converting a coin deployment to a onetime deployment.
 */
public class WranglerMigration
{
    public static class MigrationPlayerRepository extends PlayerRepository
    {
        public MigrationPlayerRepository (ConnectionProvider conprov)
            throws PersistenceException
        {
            super(conprov);
        }

        public Set<Integer> coinBuyers ()
            throws PersistenceException
        {
            return execute(new Operation<Set<Integer>>() {
                public Set<Integer> invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException, PersistenceException {
                    Set<Integer> pids = Sets.newHashSet();
                    Statement stmt = conn.createStatement();
                    try {
                        ResultSet rs = stmt.executeQuery(
                            "select PLAYER_ID from PLAYERS " +
                            "where FLAGS & " + PlayerRecord.IS_COIN_BUYER + " != 0");
                        while (rs.next()) {
                            pids.add(rs.getInt(1));
                        }
                    } finally {
                        JDBCUtil.close(stmt);
                    }
                    return pids;
                }
            });
        }
    }

    public static class MigrationItemRepository extends ItemRepository
    {
        public MigrationItemRepository (ConnectionProvider conprov)
            throws PersistenceException
        {
            super(conprov);
        }

        public Set<Integer> wranglerPassHolders ()
            throws PersistenceException
        {
            return execute(new Operation<Set<Integer>>() {
                public Set<Integer> invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException, PersistenceException {
                    Set<Integer> hids = Sets.newHashSet();
                    Statement stmt = conn.createStatement();
                    try {
                        ResultSet rs = stmt.executeQuery(
                            "select OWNER_ID from ITEMS where ITEM_TYPE = " +
                            ItemFactory.getType(GoldPass.class));
                        while (rs.next()) {
                            hids.add(rs.getInt(1));
                        }
                    } finally {
                        JDBCUtil.close(stmt);
                    }
                    return hids;
                }
            });
        }

        public void grantWranglerPasses (Set<Integer> recipIds)
            throws PersistenceException
        {
            for (int recipId : recipIds) {
                insertItem(new GoldPass(recipId, BangCodes.FRONTIER_TOWN));
            }
        }
    }

    public static class MigrationCoinRepository extends CoinRepository
    {
        public MigrationCoinRepository (ConnectionProvider conprov)
            throws PersistenceException
        {
            super(conprov, null, null, null);
        }

        public Map<String, Integer> getCoinCounts ()
            throws PersistenceException
        {
            return execute(new Operation<Map<String, Integer>>() {
                public Map<String, Integer> invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException, PersistenceException {
                    Map<String, Integer> counts = Maps.newHashMap();
                    Statement stmt = conn.createStatement();
                    try {
                        ResultSet rs = stmt.executeQuery("select ACCOUNT_NAME, COINS from COINS");
                        while (rs.next()) {
                            int coins = rs.getInt(2);
                            if (coins > 0) {
                                counts.put(rs.getString(1), coins);
                            }
                        }
                    } finally {
                        JDBCUtil.close(stmt);
                    }
                    return counts;
                }
            });
        }

        public void clearCoins (final String accName)
            throws PersistenceException
        {
            executeUpdate(new Operation<Void>() {
                public Void invoke (Connection conn, DatabaseLiaison liaison)
                    throws SQLException, PersistenceException {
                    PreparedStatement stmt = conn.prepareStatement(
                        "update COINS set COINS = 0 where ACCOUNT_NAME = ?");
                    try {
                        stmt.setString(1, accName);
                        int mods = stmt.executeUpdate();
                        if (mods != 1) {
                            System.err.println(
                                "Modified " + mods + " rows clearing '" + accName + "'.");
                        }

                    } finally {
                        JDBCUtil.close(stmt);
                    }
                    return null;
                }
            });
        }
    }

    public static void main (String[] args)
        throws Exception
    {
        Mode mode = null;
        try {
            mode = Enum.valueOf(Mode.class, args[0].toUpperCase());
        } catch (Exception e) {
            System.err.println("Usage: WranglerMigration [test|migrate]");
            System.exit(255);
        }

        ConnectionProvider conprov = new StaticConnectionProvider(ServerConfig.getJDBCConfig());
        MigrationPlayerRepository prepo = new MigrationPlayerRepository(conprov);
        MigrationItemRepository irepo = new MigrationItemRepository(conprov);
        MigrationCoinRepository crepo = new MigrationCoinRepository(conprov);

        Set<Integer> buyers = prepo.coinBuyers();
        Set<Integer> holders = irepo.wranglerPassHolders();

        // we want to grant a wrangler pass to all coin buyers that don't already hold a pass
        Set<Integer> recips = Sets.newHashSet(buyers);
        recips.removeAll(holders);
        switch (mode) {
        case TEST:
            System.out.println("Would grant pass to " + recips + ".");
            break;
        case MIGRATE:
            System.out.println("Granting pass to " + recips + ".");
            irepo.grantWranglerPasses(recips);
            break;
        }

        // we want to grant scrip to all coin holders for their coins
        Map<String, Integer> coiners = crepo.getCoinCounts();
        switch (mode) {
        case TEST:
            System.out.println("Would grant scrip to " + coiners + ".");
            break;
        case MIGRATE:
            System.out.println("Granting scrip to " + coiners + ".");
            for (Map.Entry<String, Integer> entry : coiners.entrySet()) {
                String accName = entry.getKey();
                int coins = entry.getValue();
                try {
                    prepo.grantScrip(accName, coins * SCRIP_PER_COIN);
                    crepo.clearCoins(accName);
                } catch (PersistenceException pe) {
                    System.err.println("Failed to convert coins to scrip [acc=" + accName +
                                       ", coins=" + coins + "]: " + pe);
                }
            }
            break;
        }
    }

    protected static enum Mode { TEST, MIGRATE };
    protected static final int SCRIP_PER_COIN = 400;
}
