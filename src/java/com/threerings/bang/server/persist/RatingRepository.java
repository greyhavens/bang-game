//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
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

    /**
     * Updates the supplied ratings for the specified player.
     */
    public void updateRatings (final int playerId, final ArrayList<Rating> rats)
        throws PersistenceException
    {
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String uquery = "update RATINGS set RATING=?, EXPERIENCE=? " +
                    "where PLAYER_ID=? and SCENARIO=?";
                PreparedStatement ustmt = conn.prepareStatement(uquery);
                String iquery = "insert into RATINGS (PLAYER_ID, SCENARIO, " +
                    "RATING, EXPERIENCE) values (?, ?, ?, ?)";
                PreparedStatement istmt = null;
                try {
                    for (Rating rating : rats) {
                        // first try updating
                        ustmt.setInt(1, rating.rating);
                        ustmt.setInt(2, rating.experience);
                        ustmt.setInt(3, playerId);
                        ustmt.setString(4, rating.scenario);
                        if (ustmt.executeUpdate() > 0) {
                            continue;
                        }

                        // if that didn't work, insert
                        if (istmt == null) {
                            istmt = conn.prepareStatement(iquery);
                        }
                        istmt.setInt(1, playerId);
                        istmt.setString(2, rating.scenario);
                        istmt.setInt(3, rating.rating);
                        istmt.setInt(4, rating.experience);
                        JDBCUtil.warnedUpdate(istmt, 1);
                    }

                } finally {
                    JDBCUtil.close(ustmt);
                    JDBCUtil.close(istmt);
                }
                return null;
            }
        });
    }
}
