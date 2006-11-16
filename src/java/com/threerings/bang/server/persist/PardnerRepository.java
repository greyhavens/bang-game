//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Date;

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

        /** The time of the player's last session. */
        public Date lastSession;

        /** If null the pardnership is active, if non-null the invitation
         * message from the pardner. */
        public String message;

        /** Creates a new pardner record. */
        public PardnerRecord (Handle handle, Date lastSession, String message) {
            this.handle = handle;
            this.lastSession = lastSession;
            this.message = message;
        }

        /** Returns true if this is an active record, false if it's a pending
         * invitation. */
        public boolean isActive () {
            return (message == null);
        }

        /** Returns a string representation of this instance. */
        public String toString () {
            return "[handle=" + handle + ", last=" + lastSession +
                ", message=" + message + "]";
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
    public ArrayList<PardnerRecord> getPardnerRecords (int playerId)
        throws PersistenceException
    {
        final ArrayList<PardnerRecord> list = new ArrayList<PardnerRecord>();
        final String query = PARD_SELECT +
            " where (MESSAGE is NULL and PLAYER_ID1=" + playerId +
            " and PLAYER_ID=PLAYER_ID2) union " + PARD_SELECT +
            " where (PLAYER_ID2=" + playerId + " and PLAYER_ID=PLAYER_ID1)";
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        list.add(new PardnerRecord(
                                     new Handle(rs.getString(1)),
                                     rs.getDate(2), rs.getString(3)));
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
     * @param playerId1 the id of the inviter.
     * @param handle2 the handle of the invitee.
     * @param message null if the pardnership is active, an invitation message
     * if not.
     *
     * @return null if the invitation was successfully added, otherwise a
     * translatable error message indicating what went wrong.
     */
    public String addPardners (final int playerId1, final Name handle2,
        final String message)
        throws PersistenceException
    {
        return executeUpdate(new Operation<String>() {
            public String invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // first look up the playerId for handle2
                    int playerId2 = getPlayerId(stmt, handle2);
                    if (playerId2 == -1) {
                        return MessageBundle.tcompose(
                            "e.no_such_player", handle2);
                    }

                    // now update the pardner relation between these two
                    String query = "insert ignore into PARDNERS set " +
                        "PLAYER_ID1 = " + playerId1 +
                        ", PLAYER_ID2 = " + playerId2 + ", MESSAGE = " +
                        ((message == null) ? "NULL" : JDBCUtil.escape(message));
                    if (stmt.executeUpdate(query) < 1) {
                        return MessageBundle.tcompose(
                            "e.already_invited", handle2);
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }

                return null;
            }
        });
    }

    /**
     * Confirms the status of the identified pardnership where the playerId for
     * one is known and only the name for the other is known.
     */
    public void updatePardners (final int playerId1, final Name handle2)
        throws PersistenceException
    {
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // first look up the playerId for handle2
                    int playerId2 = getPlayerId(stmt, handle2);
                    if (playerId2 == -1) {
                        log.warning("Failed to update pardners " +
                                    "[pid=" + playerId1 +
                                    ", pardner=" + handle2 + "]. " +
                                    handle2 + " no longer exists.");
                        return null;
                    }

                    // now update the pardner relation between these two
                    stmt.executeUpdate("update PARDNERS set MESSAGE = NULL" +
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
        executeUpdate(new Operation<Object>() {
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
            JDBCUtil.escape(handle.toString()));
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

    @Override // documentation inherited
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "PARDNERS", new String[] {
            "PLAYER_ID1 INTEGER UNSIGNED NOT NULL",
            "PLAYER_ID2 INTEGER UNSIGNED NOT NULL",
            "MESSAGE VARCHAR(255)",
            "UNIQUE (PLAYER_ID1, PLAYER_ID2)",
            "INDEX (PLAYER_ID2)",
        }, "");
    }

    /** Used by {@link #getPardnerRecords}. */
    protected static final String PARD_SELECT =
        "select HANDLE, LAST_SESSION, MESSAGE from PARDNERS " +
        "straight join PLAYERS";
}
