//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.TreeMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;

/**
 * Extends the {@link PlayerRepository} and provides methods for obtaining statistics that are only
 * needed by the Sheriff's Office webapp.
 */
@Singleton
public class PlayerStatRepository extends PlayerRepository
{
    /**
     * Creates the repository, runs any migrations and prepares for operation.
     */
    @Inject public PlayerStatRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov);
    }

    /**
     * Computes a summary of the last session information, reporting a date and the number of
     * players whose last session falls on that date. The first element of the list will be today's
     * date and it will proceed backward in time from there.
     */
    public TreeMap<Date,Integer> summarizeLastSessions ()
        throws PersistenceException
    {
        return summarizePlayers("LAST_SESSION");
    }

    /**
     * Computes a summary of account creation information, reporting a date and the number of
     * players who first logged in on that date. The first element of the list will be today's date
     * and it will proceed backward in time from there.
     */
    public TreeMap<Date,Integer> summarizePlayerCreation ()
        throws PersistenceException
    {
        return summarizePlayers("CREATED");
    }

    /**
     * Helper function for {@link #summarizeLastSessions} and {@link #summarizePlayerCreation}.
     */
    protected TreeMap<Date,Integer> summarizePlayers (String column)
        throws PersistenceException
    {
        final String query = "select DATE(" + column + ") as SOMEDAY, count(PLAYER_ID) " +
            "from PLAYERS group by SOMEDAY order by SOMEDAY desc";
        final TreeMap<Date,Integer> summary = new TreeMap<Date,Integer>();
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        summary.put(rs.getDate(1), rs.getInt(2));
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return summary;
    }
}
