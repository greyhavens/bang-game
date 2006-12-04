//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.util.Tuple;

/**
 * Extends the {@link PlayerRepository} and provides methods for obtaining statistics that are only
 * needed by the Sheriff's Office webapp.
 */
public class PlayerStatRepository extends PlayerRepository
{
    /**
     * Creates the repository, runs any migrations and prepares for operation.
     */
    public PlayerStatRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov);
    }

    /**
     * Computes a summary of the last session information, reporting a date and the number of
     * players whose last session falls on that date. The first element of the list will be today's
     * date and it will proceed backward in time from there.
     */
    public ArrayList<Tuple<Date,Integer>> summarizeLastSessions ()
        throws PersistenceException
    {
        final ArrayList<Tuple<Date,Integer>> summary = new ArrayList<Tuple<Date,Integer>>();
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException {
                Statement stmt = conn.createStatement();
                String query = "select DATE(LAST_SESSION) as LAST_PLAYED, count(PLAYER_ID) " +
                    "from PLAYERS group by LAST_PLAYED order by LAST_PLAYED desc";
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        summary.add(new Tuple<Date,Integer>(rs.getDate(1), rs.getInt(2)));
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
