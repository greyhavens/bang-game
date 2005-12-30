//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.SimpleRepository;

import com.threerings.bang.data.Rating;

/**
 * Responsible for the persistent storage of per-player ratings.
 */
public class RatingRepository extends SimpleRepository
{
    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>ratingdb</code>.
     */
    public static final String RATING_DB_IDENT = "ratingdb";

    /**
     * Constructs a new ratings repository with the specified connection
     * provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    public RatingRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, RATING_DB_IDENT);
    }

    /**
     * Loads the ratings for the specified player.
     */
    public ArrayList<Rating> loadRatings (final int playerId)
        throws PersistenceException
    {
        final ArrayList<Rating> rats = new ArrayList<Rating>();
        final String query = "select SCENARIO, RATING, EXPERIENCE " +
            "from RATINGS where PLAYER_ID = " + playerId;
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        Rating rat = new Rating();
                        rat.scenario = rs.getString(1);
                        rat.rating = rs.getInt(2);
                        rat.experience = rs.getInt(3);
                        rats.add(rat);
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return rats;
    }
}
