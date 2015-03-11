//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.Stats;
import com.samskivert.depot.impl.Fetcher;
import com.samskivert.depot.impl.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;

import com.threerings.stats.data.Stat;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.bang.data.StatType;

/**
 * Extends the standard StatRepository with some Bang-specific bits.
 */
@Singleton
public class BangStatRepository extends StatRepository
{
    /** Used by {@link #processStats}. */
    public interface Processor
    {
        /**
         * Called on every stat matching the criterion supplied to {@link #processStats}.
         * <em>Note:</em> do not retain a reference to the supplied {@link Stat} instance as its
         * contents will be overwritten prior to each call to process.
         */
        public void process (int playerId, String accountName, String handle,
                             Date created, int sessionMinutes, Stat stat);
    }

    /**
     * Constructs a new statistics repository with the specified persistence context.
     */
    @Inject public BangStatRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Invokes the supplied processor on every stat in the database of the specified type.
     *
     * <p><em>Note:</em> the stats database will inevitable be extremely large (one row for every
     * paying player and one for non-payers less than six months old; millions of rows if the game
     * is at all successful). Don't call this method willy nilly and the summarized results should
     * be cached for at least 12 hours. (Stats don't change that frequently in the aggregate.)
     */
    public void processStats (final Processor processor, Stat.Type type)
    {
        final Stat stat = type.newStat();
        final String query = "select STATS.PLAYER_ID, ACCOUNT_NAME, HANDLE, CREATED, " +
            "SESSION_MINUTES, STAT_DATA from STATS, PLAYERS " +
            "where PLAYERS.PLAYER_ID = STATS.PLAYER_ID and STAT_CODE = " + type.code();
        _ctx.invoke(new Fetcher.Trivial<Void>() {
            public Void invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
                throws SQLException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        if (decodeStat(stat, (byte[])rs.getObject(6), (byte)0) != null) {
                            processor.process(rs.getInt(1), rs.getString(2), rs.getString(3),
                                              rs.getDate(4), rs.getInt(5), stat);
                        }
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
            public void updateStats (Stats stats) {
                // nada
            }
        });
    }

    @Override // documentation inherited
    protected void loadStringCodes (Stat.Type type)
    {
        // we need to make sure StatType has been initialized
        @SuppressWarnings("unused") StatType stattype = StatType.UNUSED;

        // now back to our regular scheduled programming
        super.loadStringCodes(type);
    }
}
