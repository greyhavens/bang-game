//
// $Id$

package com.threerings.bang.avatar.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.SimpleRepository;

import com.threerings.bang.avatar.data.Look;

import static com.threerings.bang.Log.log;

/**
 * Stores a set of "looks" (avatar fingerprints) for players.
 */
public class LookRepository extends SimpleRepository
{
    /**
     * The database identifier used when establishing a database connection.
     * This value being <code>lookdb</code>.
     */
    public static final String LOOK_DB_IDENT = "lookdb";

    /**
     * Constructs a new look repository with the specified connection provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    public LookRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, LOOK_DB_IDENT);
    }

    /**
     * Loads up all of the different "looks" registered for this player.
     */
    public ArrayList<Look> loadLooks (int playerId)
        throws PersistenceException
    {
        final ArrayList<Look> looks = new ArrayList<Look>();
        final String query = "select NAME, AVATAR from LOOKS " +
            "where PLAYER_ID = " + playerId;
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        looks.add(decodeLook(rs));
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return looks;
    }

    /**
     * Inserts a new look into the database for the specified player.
     */
    public void insertLook (final int playerId, final Look look)
        throws PersistenceException
    {
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String ssql = "insert into LOOKS (PLAYER_ID, NAME, AVATAR) " +
                    "values (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(ssql);
                try {
                    stmt.setInt(1, playerId);
                    stmt.setString(2, look.name);
                    ByteBuffer adata = ByteBuffer.allocate(look.avatar.length*4);
                    adata.asIntBuffer().put(look.avatar);
                    stmt.setBytes(3, adata.array());
                    stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    /**
     * Deletes a particular look owned by a particular player.
     */
    public void deleteLook (int playerId, String name)
        throws PersistenceException
    {
        int mods = update("delete from LOOKS where PLAYER_ID = " + playerId +
                          " and LOOK = " + JDBCUtil.escape(name));
        if (mods != 1) {
            log.warning("Unable to delete look [pid=" + playerId +
                        ", name=" + name + ", mods=" + mods + "].");
        }
    }

    /** Helper function for {@link #loadLooks}. */
    protected Look decodeLook (ResultSet rs)
        throws SQLException
    {
        Look look = new Look();
        look.name = rs.getString(1);
        byte[] adata = rs.getBytes(2);
        IntBuffer ints = ByteBuffer.wrap(adata).asIntBuffer();
        look.avatar = new int[ints.remaining()];
        ints.get(look.avatar);
        return look;
    }
}
