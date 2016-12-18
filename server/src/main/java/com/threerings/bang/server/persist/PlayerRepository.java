//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.FieldMask;
import com.samskivert.jdbc.jora.Table;

import com.threerings.util.Name;

import com.threerings.bang.avatar.data.Look;

import com.threerings.bang.data.Handle;

import static com.threerings.bang.Log.log;

/**
 * Manages persistent information stored on a per-player basis.
 */
@Singleton
public class PlayerRepository extends JORARepository
{
    /** The database identifier used when establishing a database connection. This value being
     * <code>playerdb</code>. */
    public static final String PLAYER_DB_IDENT = "playerdb";

    /**
     * Constructs a new player repository with the specified connection provider.
     *
     * @param conprov the connection provider via which we will obtain our database connection.
     */
    @Inject public PlayerRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, PLAYER_DB_IDENT);
        _byNameMask = _ptable.getFieldMask();
        _byNameMask.setModified("accountName");
    }

    /**
     * Loads up the player record associated with the specified account.  Returns null if no
     * matching record could be found.
     */
    public PlayerRecord loadPlayer (String accountName)
        throws PersistenceException
    {
        return loadByExample(_ptable, new PlayerRecord(accountName), _byNameMask);
    }

    /**
     * Loads up the player record associated with the specified handle.  Returns null if no
     * matching record could be found.
     */
    public PlayerRecord loadByHandle (Handle handle)
        throws PersistenceException
    {
        return load(_ptable, "where NORMALIZED = " + JDBCUtil.escape(handle.getNormal()));
    }

    /**
     * Loads the words that make up all player names in the entire database. This is used by the
     * chat whitelist services.
     */
    public Set<String> loadNameWords ()
        throws PersistenceException
    {
        final Set<String> names = Sets.newHashSet();
        execute(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(
                        "select HANDLE from PLAYERS where HANDLE != NULL");
                    while (rs.next()) {
                        for (String word : rs.getString(1).split("\\s")) {
                            names.add(word);
                        }
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return names;
    }

    /**
     * Looks up the handles for all of the supplied accounts. This is used by our glue code in the
     * Underwire support system.
     */
    public Map<String,List<String>> resolveHandles (Set<String> accounts)
        throws PersistenceException
    {
        final StringBuilder query = new StringBuilder(
            "select ACCOUNT_NAME, HANDLE from PLAYERS where ACCOUNT_NAME in (");
        int idx = 0;
        for (String account : accounts) {
            if (idx++ > 0) {
                query.append(",");
            }
            query.append(JDBCUtil.escape(account));
        }
        query.append(")");

        final Map<String,List<String>> mapping = Maps.newHashMap();
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query.toString());
                    while (rs.next()) {
                        mapping.put(rs.getString(1), Lists.newArrayList(rs.getString(2)));
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return mapping;
    }

    /**
     * Insert a new player record into the repository and assigns them a unique player id in the
     * process. The {@link PlayerRecord#created} field will be filled in by this method if it is
     * not already.
     */
    public void insertPlayer (final PlayerRecord player)
        throws PersistenceException
    {
        if (player.created == null) {
            player.created = new Timestamp(System.currentTimeMillis());
            player.lastSession = player.created;
        }
        player.playerId = insert(_ptable, player);
    }

    /**
     * Configures a player's handle, and gender.
     *
     * @return true if the player was properly configured, false if the requested handle is a
     * duplicate of an existing handle.
     */
    public boolean configurePlayer (int playerId, Handle handle, boolean isMale)
        throws PersistenceException
    {
        String gensql = isMale ? ("| " + PlayerRecord.IS_MALE_FLAG) :
            ("& " + ~PlayerRecord.IS_MALE_FLAG);
        final String query = "update PLAYERS set FLAGS = FLAGS " + gensql +
            ", HANDLE = " + JDBCUtil.escape(handle.toString()) +
            ", NORMALIZED = " + JDBCUtil.escape(handle.getNormal()) +
            " where PLAYER_ID = " + playerId;
        return executeUpdate(new Operation<Boolean>() {
            public Boolean invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    int mods = stmt.executeUpdate(query);
                    if (mods != 1) {
                        log.warning("Failed to config player", "query", query, "mods", mods);
                        return Boolean.FALSE;
                    }

                } catch (SQLException sqe) {
                    if (liaison.isDuplicateRowException(sqe)) {
                        return Boolean.FALSE;
                    } else {
                        throw sqe;
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }
                return Boolean.TRUE;
            }
        });
    }

    /**
     * Marks a player as no longer being anonymous and gives them a real username.
     */
    public void clearAnonymous (int playerId, String username)
        throws PersistenceException
    {
        checkedUpdate("update PLAYERS set FLAGS = FLAGS & " + ~PlayerRecord.IS_ANONYMOUS +
                ", ACCOUNT_NAME = " + JDBCUtil.escape(username) +
                " where PLAYER_ID = " + playerId, 1);
    }

    /**
     * Marks a player as being a coin buyer.
     */
    public void markAsCoinBuyer (int playerId)
        throws PersistenceException
    {
        warnedUpdate("update PLAYERS set FLAGS = FLAGS | " + PlayerRecord.IS_COIN_BUYER +
                " where PLAYER_ID = " + playerId, 1);
    }

    /**
     * Marks a list of players as being coin buyers.
     */
    public void markAsCoinBuyers (List<String> usernames)
        throws PersistenceException
    {
        String query = "update PLAYERS set FLAGS = FLAGS | " + PlayerRecord.IS_COIN_BUYER +
            " where ACCOUNT_NAME in (" + JDBCUtil.escape(usernames.toArray()) + ")";
        warnedUpdate(query, usernames.size());
    }

    /**
     * Deletes the specified player from the repository.
     */
    public void deletePlayer (PlayerRecord player)
        throws PersistenceException
    {
        delete(_ptable, player);
    }

    /**
     * Deducts the specified amount of scrip from the specified player's account.
     */
    public void spendScrip (int playerId, int amount)
        throws PersistenceException
    {
        updateScrip("PLAYER_ID = " + playerId + " and SCRIP >= " + amount, amount, "spend");
    }

    /**
     * Adds the specified amount of scrip to the specified player's account.
     */
    public void grantScrip (int playerId, int amount)
        throws PersistenceException
    {
        updateScrip("PLAYER_ID = " + playerId, amount, "grant");
    }

    /**
     * <em>Do not use this method!</em> It exists only because we must work with the coin system
     * which tracks players by name rather than id.
     */
    public void grantScrip (String accountName, int amount)
        throws PersistenceException
    {
        updateScrip("ACCOUNT_NAME = " + JDBCUtil.escape(accountName), amount, "grant");
    }

    /**
     * Updates the specified player's record to reflect that they now have access to the specified
     * town (and all towns up to that point).
     */
    public void grantTownAccess (int playerId, String townId)
        throws PersistenceException
    {
        checkedUpdate("update PLAYERS set TOWN_ID = '" + townId + "' " +
                      "where PLAYER_ID = " + playerId, 1);
    }

    /**
     * Updates the specified player's record to reflect the timestamp up until they can freely
     * access the next town.
     */
    public void activateNextTown (int playerId, Timestamp time)
        throws PersistenceException
    {
        checkedUpdate(
                "update PLAYERS set NEXT_TOWN = '" + time + "' where PLAYER_ID = " + playerId, 1);
    }

    /**
     * Mimics the disabling of deleted players by renaming them to an invalid value that we do in
     * our user management system. This is triggered by us receiving a player action indicating
     * that the player was deleted.
     */
    public void disablePlayer (String accountName, String disabledName)
        throws PersistenceException
    {
        int mods = update(
            "update PLAYERS set ACCOUNT_NAME = " +
            JDBCUtil.escape(disabledName) + " where ACCOUNT_NAME = " +
            JDBCUtil.escape(accountName));
        switch (mods) {
        case 0:
            // they never played our game, no problem
            break;

        case 1:
            log.info("Disabled deleted player", "oname", accountName, "dname", disabledName);
            break;

        default:
            log.warning("Attempt to disable player account resulted in weirdness",
                        "aname", accountName, "dname", disabledName, "mods", mods);
            break;
        }
    }

    /**
     * Returns 5 player records that can be expired.
     */
    public ArrayList<PlayerRecord> loadExpiredPlayers (Date anon, Date user)
        throws PersistenceException
    {
        return loadAll(_ptable, "where FLAGS & " + PlayerRecord.IS_COIN_BUYER + " = 0 and " +
                "((HANDLE is NULL and LAST_SESSION < '" + anon + "') " +
                "or LAST_SESSION < '" + user + "'" +
                ") order by LAST_SESSION asc limit " + MAX_EXPIRED_PLAYERS_PER_CALL);
    }

    /**
     * Note that a user's session has ended: increment their sessions, add in the number of minutes
     * spent online, set their last session time to now and update any changed poses.
     */
    public void noteSessionEnded (int playerId, String[] poses, boolean[] changed, int minutes)
        throws PersistenceException
    {
        StringBuffer update = new StringBuffer();
        update.append("update PLAYERS set SESSIONS = SESSIONS + 1, ");
        update.append("SESSION_MINUTES = SESSION_MINUTES + ");
        update.append(minutes).append(", ");
        for (Look.Pose pose : Look.Pose.values()) {
            if (changed[pose.ordinal()]) {
                update.append(pose.getColumnName()).append(" = ");
                update.append(JDBCUtil.escape(poses[pose.ordinal()]));
                update.append(", ");
            }
        }
        update.append("LAST_SESSION = NOW() where PLAYER_ID=").append(playerId);
        checkedUpdate(update.toString(), 1);
    }

    /**
     * Loads up all folks records for the specified player.
     */
    public ArrayList<FolkRecord> loadOpinions (int playerId)
        throws PersistenceException
    {
        return loadAll(_ftable, "where PLAYER_ID = " + playerId);
    }

    /**
     * Registers an opinion of one player about another (friend or foe) or clears out any
     * registered opinion.
     *
     * @param opinion one of {@link FolkRecord#FRIEND} or {@link FolkRecord#FOE} or {@link
     * FolkRecord#NO_OPINION} if the opinion record is to be cleared.
     */
    public void registerOpinion (int playerId, int targetId, byte opinion)
        throws PersistenceException
    {
        FolkRecord frec = new FolkRecord();
        frec.playerId = playerId;
        frec.targetId = targetId;
        frec.opinion = opinion;
        if (frec.opinion == FolkRecord.NO_OPINION) {
            delete(_ftable, frec);
        } else {
            store(_ftable, frec); // this will update or insert
        }
    }

    /**
     * Clears all registered opinions on/of the target player.
     */
    public void clearOpinions (int playerId)
        throws PersistenceException
    {
        update("delete from FOLKS where PLAYER_ID = " + playerId + " or TARGET_ID = " + playerId);
    }

    /**
     * Returns the player id corresponding to the given handle, or -1 if no such player exists.
     */
    public int getPlayerId (Statement stmt, Name handle)
        throws SQLException, PersistenceException
    {
        int playerId = -1;
        ResultSet rs = stmt.executeQuery(
            "select PLAYER_ID from PLAYERS where NORMALIZED = " +
            JDBCUtil.escape(handle.getNormal()));
        while (rs.next()) {
            playerId = rs.getInt(1);
        }
        rs.close();
        return playerId;
    }

    /**
     * Returns the account name of the identified player, or <code>null</code> if no such player
     * exists.
     */
    public String getAccountName (int playerId)
        throws PersistenceException
    {
        final String query = "select ACCOUNT_NAME from PLAYERS where PLAYER_ID = " + playerId;
        return executeUpdate(new Operation<String>() {
            public String invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    return (rs.next() ? rs.getString(1) : null);
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Sets a temp ban expiration date and reason on an account.
     */
    public void setTempBan (String accountName, Timestamp expires, String warning)
        throws PersistenceException
    {
        String query = "update PLAYERS set BAN_EXPIRES = '" + expires + "', WARNING = " +
            JDBCUtil.escape(warning) + " where ACCOUNT_NAME = " + JDBCUtil.escape(accountName);
        checkedUpdate(query, 1);
    }

    /**
     * Sets a warning on an account.
     */
    public void setWarning (String accountName, String warning)
        throws PersistenceException
    {
        String query = "update PLAYERS set WARNING = " + JDBCUtil.escape(warning) +
            " where ACCOUNT_NAME = " + JDBCUtil.escape(accountName);
        checkedUpdate(query, 1);
    }

    /**
     * Clears a temp ban from an account.
     */
    public void clearTempBan (int playerId)
        throws PersistenceException
    {
        String query = "update PLAYERS set BAN_EXPIRES = NULL where PLAYER_ID = " + playerId;
        checkedUpdate(query, 1);
    }

    /**
     * Clears a warning from an account.
     */
    public void clearWarning (int playerId)
        throws PersistenceException
    {
        String query = "update PLAYERS set WARNING = NULL where PLAYER_ID = " + playerId;
        checkedUpdate(query, 1);
    }

    /** Helper function for {@link #spendScrip} and {@link #grantScrip}. */
    protected void updateScrip (String where, int amount, String type)
        throws PersistenceException
    {
        if (amount <= 0) {
            throw new PersistenceException(
                "Illegal scrip " + type + " [where=" + where + ", amount=" + amount + "]");
        }

        String action = type.equals("grant") ? "+" : "-";
        String query = "update PLAYERS set SCRIP = SCRIP " + action + " " + amount +
            " where " + where;
        int mods = update(query);
        if (mods == 0) {
            throw new PersistenceException("Scrip " + type + " modified zero rows [where=" + where +
                                           ", amount=" + amount + "]");
        } else if (mods > 1) {
            log.warning("Scrip " + type + " modified multiple rows", "where", where,
                        "amount", amount, "mods", mods);
        }
    }

    @Override // documentation inherited
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "PLAYERS", new String[] {
            "PLAYER_ID INTEGER NOT NULL AUTO_INCREMENT",
            "ACCOUNT_NAME VARCHAR(64) NOT NULL",
            "HANDLE VARCHAR(64)",
            "NORMALIZED VARCHAR(64) UNIQUE",
            "SCRIP INTEGER NOT NULL",
            "LOOK VARCHAR(" + Look.MAX_NAME_LENGTH + ") NOT NULL",
            "VICTORY_LOOK VARCHAR(" + Look.MAX_NAME_LENGTH + ") NOT NULL",
            "WANTED_LOOK VARCHAR(" + Look.MAX_NAME_LENGTH + ") NOT NULL",
            "TOWN_ID VARCHAR(64) NOT NULL",
            "NEXT_TOWN DATETIME DEFAULT NULL",
            "CREATED DATETIME NOT NULL",
            "SESSIONS INTEGER NOT NULL",
            "SESSION_MINUTES INTEGER NOT NULL",
            "LAST_SESSION DATETIME NOT NULL",
            "FLAGS INTEGER NOT NULL",
            "BAN_EXPIRES DATETIME DEFAULT NULL",
            "WARNING VARCHAR(255)",
            "PRIMARY KEY (PLAYER_ID)",
            "UNIQUE (ACCOUNT_NAME)",
            "INDEX (LAST_SESSION)",
        }, "");

        JDBCUtil.createTableIfMissing(conn, "FOLKS", new String[] {
            "PLAYER_ID INTEGER NOT NULL",
            "TARGET_ID INTEGER NOT NULL",
            "OPINION TINYINT NOT NULL",
            "KEY (PLAYER_ID)",
            "UNIQUE (PLAYER_ID, TARGET_ID)",
        }, "");

        // TEMP: remove gang fields
        JDBCUtil.dropColumn(conn, "PLAYERS", "GANG_ID");
        JDBCUtil.dropColumn(conn, "PLAYERS", "GANG_RANK");
        JDBCUtil.dropColumn(conn, "PLAYERS", "JOINED_GANG");
        // END TEMP

        // TEMP: add normalized column
        if (!JDBCUtil.tableContainsColumn(conn, "PLAYERS", "NORMALIZED")) {
            JDBCUtil.addColumn(conn, "PLAYERS", "NORMALIZED", "VARCHAR(64) UNIQUE", "HANDLE");
            Statement stmt = conn.createStatement();
            try {
                stmt.executeUpdate("drop index HANDLE on PLAYERS");
                // NOTE: all collisions must be removed by hand before this is run or it will fail
                stmt.executeUpdate(
                    "update PLAYERS set NORMALIZED = LOWER(REPLACE(HANDLE, \" \", \"\")) " +
                    "where HANDLE is NOT NULL");
            } finally {
                stmt.close();
            }
        }
        // END TEMP

        // TEMP: add next town timestamp column
        if (!JDBCUtil.tableContainsColumn(conn, "PLAYERS", "NEXT_TOWN")) {
            JDBCUtil.addColumn(conn, "PLAYERS", "NEXT_TOWN", "DATETIME DEFAULT NULL", "TOWN_ID");
        }
        // ENT TEMP

        // TEMP: add PLAYERS.LAST_SESSION index
        JDBCUtil.addIndexToTable(conn, "PLAYERS", "LAST_SESSION", "LAST_SESSION_INDEX");
        // END TEMP

        // TEMP: add temp ban columns
        if (!JDBCUtil.tableContainsColumn(conn, "PLAYERS", "BAN_EXPIRES")) {
            JDBCUtil.addColumn(conn, "PLAYERS", "BAN_EXPIRES", "DATETIME DEFAULT NULL", "FLAGS");
            JDBCUtil.addColumn(conn, "PLAYERS", "WARNING", "VARCHAR(255)", "BAN_EXPIRES");
        }
        // END TEMP
    }

    @Override // documentation inherited
    protected void createTables ()
    {
	_ptable = new Table<PlayerRecord>(PlayerRecord.class, "PLAYERS", "PLAYER_ID", true);
	_ftable = new Table<FolkRecord>(FolkRecord.class, "FOLKS", new String[] {
            "PLAYER_ID", "TARGET_ID" }, true);
    }

    protected Table<PlayerRecord> _ptable;
    protected Table<FolkRecord> _ftable;
    protected FieldMask _byNameMask;

    /** The maximum number of expired players we'll return in a single call (as the server will go
     * on to delete all the players we return and we don't want to overload it). */
    protected static final int MAX_EXPIRED_PLAYERS_PER_CALL = 5;
}
