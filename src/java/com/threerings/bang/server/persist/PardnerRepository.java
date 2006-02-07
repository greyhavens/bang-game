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

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.bang.data.Handle;

import static com.threerings.bang.Log.*;

/**
 * Persistifies pardner relationships.
 */
public class PardnerRepository extends SimpleRepository
{
    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>pardnerdb</code>.
     */
    public static final String PARDNER_DB_IDENT = "pardnerdb";

    /** Contains information loaded from the database about a pardnership. */
    public static class PardnerRecord
    {
        /** The handle of the other player. */
        public Handle handle;
        
        /** Whether the pardnership is active or merely proposed. */
        public boolean active;
        
        public PardnerRecord (Handle handle, boolean active)
        {
            this.handle = handle;
            this.active = active;
        }
    }
    
    /**
     * Constructs a new pardner repository with the specified connection
     * provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    public PardnerRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, PARDNER_DB_IDENT);
    }

    /**
     * Get a list of {@link PardnerRecord}s representing all pardnerships
     * (active and proposed) involving the specified player.
     */
    public ArrayList<PardnerRecord> getPardnerRecords (final int playerId)
        throws PersistenceException
    {
        final ArrayList<PardnerRecord> list = new ArrayList<PardnerRecord>();
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // this is actually faster than two queries
                    ResultSet rs = stmt.executeQuery(
                        "select HANDLE, ACTIVE from PARDNERS straight join " +
                        "PLAYERS where (ACTIVE=1 and PLAYER_ID1=" + playerId +
                        " and PLAYER_ID=PLAYER_ID2) or (PLAYER_ID2=" +
                        playerId + " and PLAYER_ID=PLAYER_ID1)");
                    while (rs.next()) {
                        list.add(new PardnerRecord(
                            new Handle(JDBCUtil.unjigger(rs.getString(1))),
                            rs.getBoolean(2)));
                    }
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
        return list;
    }

    /**
     * Adds a pardnership relation to the database.
     *
     * @param playerId1 the id of the inviter
     * @param handle2 the handle of the invitee
     * @param active whether the pardnership is active or merely proposed
     * @return null if the invitation was successfully added, otherwise a
     * translatable error message indicating what went wrong
     */
    public String addPardners (final int playerId1, final Name handle2,
        final boolean active)
        throws PersistenceException
    {
        return (String)execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // first look up the playerId for handle2
                    int playerId2 = getPlayerId(stmt, handle2);
                    if (playerId2 == -1) {
                        return MessageBundle.tcompose("e.no_such_player",
                            handle2);
                    }

                    // now update the pardner relation between these two
                    if (stmt.executeUpdate("insert ignore into PARDNERS set " +
                        "PLAYER_ID1=" + playerId1 + ", PLAYER_ID2=" +
                        playerId2 + ", ACTIVE=" + (active ? "1" : "0")) < 1) {
                        return MessageBundle.tcompose("e.already_invited",
                            handle2);
                    }
                    
                } finally {
                    JDBCUtil.close(stmt);
                }

                return null;
            }
        });
    }
    
    /**
     * Updates the status of the identified pardnership where the playerId for
     * one is known and only the name for the other is known.
     */
    public void updatePardners (final int playerId1, final Name handle2,
        final boolean active)
        throws PersistenceException
    {
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // first look up the playerId for handle2
                    int playerId2 = getPlayerId(stmt, handle2);
                    if (playerId2 == -1) {
                        log.warning("Failed to update pardners [pid=" +
                            playerId1 + ", pardner=" + handle2 + ", active=" +
                            active + "]. Pardner no longer exists.");
                        return null;
                    }

                    // now update the pardner relation between these two
                    stmt.executeUpdate("update PARDNERS set ACTIVE = " +
                        (active ? "1" : "0") +
                        createWhereClause(playerId1, playerId2));

                } finally {
                    JDBCUtil.close(stmt);
                }

                return null;
            }
        });
    }
    
    /**
     * Remove a pardner mapping from the database where the playerId for one
     * is known and only the name for the other is known.
     */
    public void removePardners (final int playerId1, final Name handle2)
        throws PersistenceException
    {
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // first look up the playerId for handle2
                    int playerId2 = getPlayerId(stmt, handle2);
                    if (playerId2 == -1) {
                        log.warning("Failed to delete pardners [pid=" +
                            playerId1 + ", pardner=" + handle2 + "]. " +
                            "Pardner no longer exists.");
                        return null;
                    }

                    // now delete any pardner relation between these two
                    stmt.executeUpdate("delete from PARDNERS" +
                        createWhereClause(playerId1, playerId2));

                } finally {
                    JDBCUtil.close(stmt);
                }

                return null;
            }
        });
    }
    
    /**
     * Returns the player id corresponding to the given handle, or -1
     * if no such player exists.
     */
    protected int getPlayerId (Statement stmt, Name handle)
        throws SQLException, PersistenceException
    {
        int playerId = -1;
        ResultSet rs = stmt.executeQuery(
            "select PLAYER_ID from PLAYERS where HANDLE = " +
            JDBCUtil.escape(JDBCUtil.jigger(
                handle.toString())));
        while (rs.next()) {
            playerId = rs.getInt(1);
        }
        rs.close();
        return playerId;
    }
    
    /**
     * Creates a 'where' clause that matches the two given player ids in either
     * order.
     */
    protected String createWhereClause (int playerId1, int playerId2)
    {
        return " where (PLAYER_ID1 = " + playerId1 + " and PLAYER_ID2 = " +
            playerId2 + ") or (PLAYER_ID1 = " + playerId2 + " and " +
            "PLAYER_ID2 = " + playerId1 + ")";
    }
}
