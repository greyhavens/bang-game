//
// $Id$

package com.threerings.bang.gang.server.persist;

import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Collections;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.FieldMask;
import com.samskivert.jdbc.jora.Table;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Handle;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HistoryEntry;

import static com.threerings.bang.Log.*;

/**
 * Persistifies gang information.
 */
public class GangRepository extends JORARepository
{
    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>gangdb</code>.
     */
    public static final String GANG_DB_IDENT = "gangdb";
    
    /** Contains information loaded from the database about a gang. */
    public static class GangRecord
    {
        /** The gang's unique identifier. */
        public int gangId;
    
        /** The name of the gang. */
        public String name;

        /** The date upon which the gang was founded. */
        public Timestamp founded;

        /** The gang's accumulated notoriety points. */
        public int notoriety;
        
        /** The amount of scrip in the gang's coffers. */
        public int scrip;
        
        /** The number of coins in the gang's coffers. */
        public int coins;
        
        /** The encoded brand. */
        public byte[] brand;
        
        /** The encoded outfit. */
        public byte[] outfit;
        
        /** The members of the gang. */
        public transient ArrayList<GangMemberEntry> members;
        
        /** Used when creating new gangs. */
        public GangRecord (String name)
        {
            this.name = name;
        }

        /** Used when forming queries. */
        public GangRecord (int gangId)
        {
            this.gangId = gangId;
        }
        
        /** Used when loading records from the database. */
        public GangRecord ()
        {
        }
        
        /** Creates and populates (but does not register) a distributed object
         * using the information in this record. */
        public GangObject createGangObject ()
        {
            GangObject gang = new GangObject();
            gang.gangId = gangId;
            gang.name = getName();
            gang.founded = founded.getTime();
            gang.scrip = scrip;
            gang.coins = coins;
            if (members != null) {
                gang.members = new DSet<GangMemberEntry>(members.iterator());
            }
            return gang;
        }
        
        /** Returns the gang name as a {@link Handle}. */
        public Handle getName ()
        {
            return new Handle(name);
        }
    
        /** Returns a string representation of this instance. */
        public String toString ()
        {
            return "[gangId=" + gangId + ", name=" + name + ", founded=" +
                founded + ", scrip=" + scrip + ", coins=" + coins + "]";
        }
    }

    /** Contains information loaded from the database about a gang member. */
    public static class MemberRecord
    {
        /** The member's player id. */
        public int playerId;
        
        /** The id of the gang to which the player belongs. */
        public int gangId;
        
        /** The player's rank in the gang. */
        public byte rank;
        
        /** The time at which the player joined the gang. */
        public Timestamp joined;
        
        /** The player's total contribution to the gang's notoriety. */
        public int notoriety;
        
        /** Used when adding new members. */
        public MemberRecord (int playerId, int gangId, byte rank)
        {
            this.playerId = playerId;
            this.gangId = gangId;
            this.rank = rank;
        }

        /** Used when forming queries. */
        public MemberRecord (int playerId)
        {
            this.playerId = playerId;
        }
        
        /** Used when loading records from the database. */
        public MemberRecord ()
        {
        }
    }
    
    /** Contains information loaded from the database about a gang invitation. */
    public static class InviteRecord
    {
        /** The name of the player extending the invitation. */
        public Handle inviter;

        /** The id of the gang to which the player has been invited. */
        public int gangId;
        
        /** The name of the gang to which the player has been invited. */
        public Handle name;
        
        /** The text of the invitation. */
        public String message;
        
        /** Creates a new invitation. */
        public InviteRecord (Handle inviter, int gangId, Handle name, String message)
        {
            this.inviter = inviter;
            this.gangId = gangId;
            this.name = name;
            this.message = message;
        }
    }
    
    /** Contains information loaded from the database about a historical event. */
    public static class HistoryRecord
    {
        /** The entry's unique identifier. */
        public int entryId;
        
        /** The gang to which the entry refers. */
        public int gangId;
        
        /** The time at which the event was recorded. */
        public Timestamp recorded;
        
        /** The event description. */
        public String description;
        
        /** Used when adding new events. */
        public HistoryRecord (int gangId, String description)
        {
            this.gangId = gangId;
            this.description = description;
        }

        /** Used when forming queries. */
        public HistoryRecord (int entryId)
        {
            this.entryId = entryId;
        }
    }
    
    /**
     * Constructs a new gang repository with the specified connection
     * provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    public GangRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, GANG_DB_IDENT);
        _gangIdMask = _gtable.getFieldMask();
        _gangIdMask.setModified("gangId");
        _playerIdMask = _mtable.getFieldMask();
        _playerIdMask.setModified("playerId");
    }
    
    /**
     * Loads up the gang record associated with the specified id.
     * Returns null if no matching record could be found.
     *
     * @param members if true, load the member entries as well and store
     * them in the record
     */
    public GangRecord loadGang (int gangId, boolean members)
        throws PersistenceException
    {
        GangRecord grec = loadByExample(
            _gtable, new GangRecord(gangId), _gangIdMask);
        if (grec != null && members) {
            grec.members = loadGangMembers(gangId);
        }
        return grec;
    }
    
    /**
     * Insert a new gang record into the repository and assigns a unique
     * gang id in the process. The {@link GangRecord#founded} field will be
     * filled in by this method if it is not already.
     */
    public void insertGang (GangRecord gang)
        throws PersistenceException
    {
        if (gang.founded == null) {
            gang.founded = new Timestamp(System.currentTimeMillis());
        }
        gang.gangId = insert(_gtable, gang);
    }
    
    /**
     * Adds or removes cash to/from the gang's coffers.
     */
    public void addToCoffers (int gangId, int scrip, int coins)
        throws PersistenceException
    {
        checkedUpdate("update GANGS set SCRIP = SCRIP + " + scrip +
                      ", COINS = COINS + " + coins + " where GANG_ID = " +
                      gangId, 1);
    }
    
    /**
     * Adds notoriety points to the gang and user records.
     */
    public void addNotoriety (int gangId, int playerId, int points)
        throws PersistenceException
    {
        checkedUpdate("update GANGS set NOTORIETY = NOTORIETY + " + points +
                      " where GANG_ID = " + gangId, 1);
        checkedUpdate("update GANG_MEMBERS set NOTORIETY = NOTORIETY + " + points +
                      " where PLAYER_ID = " + playerId, 1);
    }
    
    /**
     * Deletes a gang from the repository.
     */
    public void deleteGang (int gangId)
        throws PersistenceException
    {
        delete(_gtable, new GangRecord(gangId));
        
        // delete all of the gang's invites and history entries
        update("delete from GANG_INVITES where GANG_ID = " + gangId);
        update("delete from GANG_HISTORY where GANG_ID = " + gangId);
    }
    
    /**
     * Loads the membership record for the specified player.
     *
     * @return the loaded record, or <code>null</code> if the user does not
     * belong to a gang.
     */
    public MemberRecord loadMember (int playerId)
        throws PersistenceException
    {
        return loadByExample(_mtable, new MemberRecord(playerId), _playerIdMask);
    }
    
    /**
     * Inserts a new membership record.  The {@link MemberRecord#joined} field will be
     * filled in by this method if it is not already.
     */
    public void insertMember (MemberRecord member)
        throws PersistenceException
    {
        if (member.joined == null) {
            member.joined = new Timestamp(System.currentTimeMillis());
        }
        insert(_mtable, member);
    }
    
    /** 
     * Deletes the specified user's membership record.
     */
    public void deleteMember (int playerId)
        throws PersistenceException
    {
        delete(_mtable, new MemberRecord(playerId));
    }
    
    /**
     * Changes the specified user's rank.
     */
    public void updateRank (int playerId, byte rank)
        throws PersistenceException
    {
        checkedUpdate("update GANG_MEMBERS set RANK = " + rank +
                      " where PLAYER_ID = " + playerId, 1);
    }
    
    /**
     * Get a list of {@link InviteRecord}s representing all invitations stored
     * for the specified player.
     */
    public ArrayList<InviteRecord> getInviteRecords (int playerId)
        throws PersistenceException
    {
        final ArrayList<InviteRecord> list = new ArrayList<InviteRecord>();
        final String query = "select HANDLE, GANG_INVITES.GANG_ID, NAME, MESSAGE from " +
            "GANG_INVITES, PLAYERS, GANGS where GANG_INVITES.INVITER_ID = PLAYERS.PLAYER_ID " +
            "and GANG_INVITES.GANG_ID = GANGS.GANG_ID and GANG_INVITES.PLAYER_ID = " + playerId;
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        list.add(new InviteRecord(
                                     new Handle(rs.getString(1)),
                                     rs.getInt(2),
                                     new Handle(rs.getString(3)),
                                     rs.getString(4)));
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
     * Adds an invitation for a player to join a gang.
     *
     * @return null if the invitation was successfully added, otherwise a
     * translatable error message indicating what went wrong.
     */
    public String insertInvite (
        final int inviterId, final int gangId, final Handle handle,
        final String message)
        throws PersistenceException
    {
        return executeUpdate(new Operation<String>() {
            public String invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // first look up the playerId
                    int playerId = BangServer.playrepo.getPlayerId(stmt, handle);
                    if (playerId == -1) {
                        return MessageBundle.tcompose("e.no_such_player", handle);
                    }
                    
                    // then the current gang
                    ResultSet rs = stmt.executeQuery("select GANG_ID from GANG_MEMBERS " +
                        "where PLAYER_ID = " + playerId);
                    if (rs.next()) {
                        return MessageBundle.tcompose("e.already_member_other", handle);
                    }
                    
                    // attempt to insert the invitation
                    String query = "insert ignore into GANG_INVITES set " +
                        "INVITER_ID = " + inviterId + ", GANG_ID = " + gangId +
                        ", PLAYER_ID = " + playerId + ", MESSAGE = " +
                        JDBCUtil.escape(message);
                    if (stmt.executeUpdate(query) < 1) {
                        return MessageBundle.tcompose(
                            "e.already_invited", handle);
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }

                return null;
            }
        });
    }
    
    /**
     * Processes and removes an invitation from the database.
     *
     * @return <code>null</code> if the operation succeeded, otherwise an error message
     * indicating what went wrong.
     */
    public String deleteInvite (final int gangId, final int playerId, final boolean accepted)
        throws PersistenceException
    {
        return executeUpdate(new Operation<String>() {
            public String invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    int rows = stmt.executeUpdate("delete from GANG_INVITES where " +
                        "GANG_ID = " + gangId + " and PLAYER_ID = " + playerId);
                    if (!accepted) {
                        // it's fine to "reject" deleted invites
                        return null;
                        
                    } else if (rows < 1) {
                        return "e.invite_removed";
                    }
                    
                    // if the gang is going to be full, remove all pending invites
                    ResultSet rs = stmt.executeQuery(
                        "select count(*) from GANG_MEMBERS where GANG_ID = " + gangId);
                    if (rs.next() && rs.getInt(1) >= GangCodes.MAX_MEMBERS - 1) {
                        stmt.executeUpdate("delete from GANG_INVITES where GANG_ID = " + gangId);
                    }
                    
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }
    
    /**
     * Inserts a new historical entry into the database.
     *
     * @return the unique id assigned to the entry, which can be used to delete it.
     */
    public int insertHistoryEntry (int gangId, String description)
        throws PersistenceException
    {
        return insert(_htable, new HistoryRecord(gangId, description));
    }
    
    /**
     * Deletes a historical entry from the database.
     */
    public void deleteHistoryEntry (final int entryId)
        throws PersistenceException
    {
        delete(_htable, new HistoryRecord(entryId));
    }
    
    /**
     * Loads a batch of historical entries from the database.
     *
     * @param offset the offset from the end (e.g., 0 to retrieve the last <code>count</code>
     * entries, <code>count</code> to retrieve the next-to-last <code>count</code>)
     */
    public ArrayList<HistoryEntry> loadHistoryEntries (int gangId, int offset, int count)
        throws PersistenceException
    {
        final ArrayList<HistoryEntry> list = new ArrayList<HistoryEntry>();
        final String query = "select RECORDED, DESCRIPTION from GANG_HISTORY where GANG_ID = " +
            gangId + " order by ENTRY_ID desc limit " + offset + ", " + count;
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        list.add(new HistoryEntry(rs.getTimestamp(1), rs.getString(2)));
                    }
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
        Collections.reverse(list); // return to chronological order
        return list;
    }
    
    /**
     * Loads the entries for all members of the specified gang.
     */
    protected ArrayList<GangMemberEntry> loadGangMembers (int gangId)
        throws PersistenceException
    {
        final ArrayList<GangMemberEntry> list = new ArrayList<GangMemberEntry>();
        final String query = "select GANG_MEMBERS.PLAYER_ID, HANDLE, RANK, " +
            "JOINED, LAST_SESSION from GANG_MEMBERS, PLAYERS where " +
            "GANG_MEMBERS.PLAYER_ID = PLAYERS.PLAYER_ID and GANG_ID = " + gangId;
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        list.add(new GangMemberEntry(
                            rs.getInt(1), new Handle(rs.getString(2)),
                            rs.getByte(3), rs.getTimestamp(4), rs.getTimestamp(5)));
                    }
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
        return list;
    }
    
    @Override // documentation inherited
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "GANGS", new String[] {
            "GANG_ID INTEGER NOT NULL AUTO_INCREMENT",
            "NAME VARCHAR(64) NOT NULL",
            "FOUNDED DATETIME NOT NULL",
            "NOTORIETY INTEGER NOT NULL",
            "SCRIP INTEGER NOT NULL",
            "COINS INTEGER NOT NULL",
            "BRAND BLOB",
            "OUTFIT BLOB",
            "PRIMARY KEY (GANG_ID)",
            "UNIQUE (NAME)",
        }, "");
        
        // TEMP: change brand/outfit column types
        if (!JDBCUtil.isColumnNullable(conn, "GANGS", "BRAND")) {
            JDBCUtil.changeColumn(conn, "GANGS",
                "BRAND", "BRAND BLOB");
            JDBCUtil.changeColumn(conn, "GANGS",
                "OUTFIT", "OUTFIT BLOB");
        }
        // END TEMP
        
        // TEMP: add the notoriety column
        JDBCUtil.addColumn(conn, "GANGS", "NOTORIETY", "INTEGER NOT NULL", "FOUNDED");
        // END TEMP
        
        JDBCUtil.createTableIfMissing(conn, "GANG_MEMBERS", new String[] {
            "PLAYER_ID INTEGER NOT NULL",
            "GANG_ID INTEGER NOT NULL",
            "RANK TINYINT NOT NULL",
            "JOINED DATETIME NOT NULL",
            "NOTORIETY INTEGER NOT NULL",
            "PRIMARY KEY (PLAYER_ID)",
            "INDEX (GANG_ID)",
        }, "");
        
        JDBCUtil.createTableIfMissing(conn, "GANG_INVITES", new String[] {
            "INVITER_ID INTEGER NOT NULL",
            "GANG_ID INTEGER NOT NULL",
            "PLAYER_ID INTEGER NOT NULL",
            "MESSAGE VARCHAR(255) NOT NULL",
            "UNIQUE (GANG_ID, PLAYER_ID)",
            "INDEX (PLAYER_ID)",
        }, "");
        
        JDBCUtil.createTableIfMissing(conn, "GANG_HISTORY", new String[] {
            "ENTRY_ID INTEGER NOT NULL AUTO_INCREMENT",
            "GANG_ID INTEGER NOT NULL",
            "RECORDED TIMESTAMP NOT NULL",
            "DESCRIPTION TEXT NOT NULL",
            "PRIMARY KEY (ENTRY_ID)",
            "INDEX (GANG_ID)",
        }, "");
    }
    
    @Override // documentation inherited
    protected void createTables ()
    {
	    _gtable = new Table<GangRecord>(GangRecord.class, "GANGS", "GANG_ID", true);
	    _mtable = new Table<MemberRecord>(MemberRecord.class, "GANG_MEMBERS", "PLAYER_ID", true);
	    _htable = new Table<HistoryRecord>(HistoryRecord.class, "GANG_HISTORY", "ENTRY_ID", true);
    }
    
    protected Table<GangRecord> _gtable;
    protected Table<MemberRecord> _mtable;
    protected Table<HistoryRecord> _htable;
    protected FieldMask _gangIdMask, _playerIdMask;
}
