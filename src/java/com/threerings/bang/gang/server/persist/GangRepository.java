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

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Handle;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.PlayerRecord;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangEntry;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HistoryEntry;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.gang.data.TopRankedGangList;

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

    /**
     * Constructs a new gang repository with the specified connection provider.
     *
     * @param conprov the connection provider via which we will obtain our database connection.
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
     * Loads directory entries for all active gangs.
     */
    public ArrayList<GangEntry> loadGangs ()
        throws PersistenceException
    {
        final ArrayList<GangEntry> list = new ArrayList<GangEntry>();
        final String query = "select NAME from GANGS";
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        list.add(new GangEntry(new Handle(rs.getString(1))));
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
     * Loads up the gang record associated with the specified id.  Returns null if no matching
     * record could be found.
     *
     * @param all if true, load the member entries and the rest of the gang's data and store it in
     * the gang record
     */
    public GangRecord loadGang (int gangId, boolean all)
        throws PersistenceException
    {
        GangRecord grec = loadByExample(_gtable, new GangRecord(gangId), _gangIdMask);
        if (grec != null && all) {
            grec.members = loadGangMembers(gangId);
            
            // load the outfit
            ArrayList<GangOutfitRecord> recs = loadAll(_otable, "where GANG_ID = " + gangId);
            grec.outfit = new OutfitArticle[recs.size()];
            for (int ii = 0; ii < grec.outfit.length; ii++) {
                GangOutfitRecord rec = recs.get(ii);
                grec.outfit[ii] = new OutfitArticle(rec.article, rec.zations);
            }
            
            // load the avatar for the most senior member
            GangMemberEntry senior = null;
            for (GangMemberEntry entry : grec.members) {
                if (entry.rank == GangCodes.LEADER_RANK &&
                    (senior == null || entry.rank < senior.rank)) {
                    senior = entry;
                }
            }
            grec.avatar = BangServer.lookrepo.loadSnapshot(senior.playerId);
        }
        return grec;
    }

    /**
     * Insert a new gang record into the repository and assigns a unique gang id in the process.
     * The {@link GangRecord#founded} field will be filled in by this method if it is not already.
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
     * Updates a gang's statement and URL.
     */
    public void updateStatement (int gangId, String statement, String url)
        throws PersistenceException
    {
        checkedUpdate("update GANGS set STATEMENT = " + JDBCUtil.escape(statement) +
            ", URL = " + JDBCUtil.escape(url) + " where GANG_ID = " + gangId, 1);
    }
    
    /**
     * Adds or removes cash to/from the gang's coffers.
     */
    public void addToCoffers (int gangId, int scrip, int coins)
        throws PersistenceException
    {
        checkedUpdate("update GANGS set SCRIP = SCRIP + " + scrip +
                      ", COINS = COINS + " + coins + " where GANG_ID = " + gangId, 1);
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

        // delete all of the gang's invites, history entries, outfits
        update("delete from GANG_INVITES where GANG_ID = " + gangId);
        update("delete from GANG_HISTORY where GANG_ID = " + gangId);
        update("delete from GANG_OUTFITS where GANG_ID = " + gangId);
    }

    /**
     * Loads the membership record for the specified player.
     *
     * @return the loaded record, or <code>null</code> if the user does not belong to a gang.
     */
    public GangMemberRecord loadMember (int playerId)
        throws PersistenceException
    {
        return loadByExample(_mtable, new GangMemberRecord(playerId), _playerIdMask);
    }

    /**
     * Loads the ids of gang members by gender.
     *
     * @param maleIds the set to populate with the ids of male members
     * @param femaleIds the set to populate with the ids of female members
     */
    public void loadMemberIds (
        int gangId, final ArrayIntSet maleIds, final ArrayIntSet femaleIds)
        throws PersistenceException
    {
        final String query = "select GANG_MEMBERS.PLAYER_ID, FLAGS from GANG_MEMBERS, PLAYERS " +
            "where GANG_MEMBERS.PLAYER_ID = PLAYERS.PLAYER_ID and GANG_ID = " + gangId;
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        int playerId = rs.getInt(1), flags = rs.getInt(2);
                        if ((flags & PlayerRecord.IS_MALE_FLAG) != 0) {
                            maleIds.add(playerId);
                        } else {
                            femaleIds.add(playerId);
                        }
                    }
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }
    
    /**
     * Inserts a new membership record.  The {@link GangMemberRecord#joined} field will be filled
     * in by this method if it is not already.
     */
    public void insertMember (GangMemberRecord member)
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
        delete(_mtable, new GangMemberRecord(playerId));
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
     * Get a list of {@link GangInviteRecord}s representing all invitations stored for the
     * specified player.
     */
    public ArrayList<GangInviteRecord> getInviteRecords (int playerId)
        throws PersistenceException
    {
        final ArrayList<GangInviteRecord> list = new ArrayList<GangInviteRecord>();
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
                        list.add(new GangInviteRecord(
                                     new Handle(rs.getString(1)), rs.getInt(2),
                                     new Handle(rs.getString(3)), rs.getString(4)));
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
     * @return null if the invitation was successfully added, otherwise a translatable error
     * message indicating what went wrong.
     */
    public String insertInvite (
        final int inviterId, final int gangId, final Handle handle, final String message)
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
                        ", PLAYER_ID = " + playerId + ", MESSAGE = " + JDBCUtil.escape(message);
                    if (stmt.executeUpdate(query) < 1) {
                        return MessageBundle.tcompose("e.already_invited", handle);
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
     * @return <code>null</code> if the operation succeeded, otherwise an error message indicating
     * what went wrong.
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
        return insert(_htable, new GangHistoryRecord(gangId, description));
    }

    /**
     * Deletes a historical entry from the database.
     */
    public void deleteHistoryEntry (final int entryId)
        throws PersistenceException
    {
        delete(_htable, new GangHistoryRecord(entryId));
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
        final String query = "select RECORDED, DESCRIPTION from GANG_HISTORY " +
            "where GANG_ID = " + gangId + " order by ENTRY_ID desc limit " + offset + ", " + count;
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
     * Loads the top-ranked gangs in terms of notoriety.
     */
    public TopRankedGangList loadTopRankedByNotoriety (int count)
        throws PersistenceException
    {
        final ArrayList<Handle> names = new ArrayList<Handle>();
        final String query = "select NAME from GANGS order by NOTORIETY desc limit " + count;
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        names.add(new Handle(rs.getString(1)));
                    }
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
        TopRankedGangList list = new TopRankedGangList();
        list.criterion = "m.top_notoriety";
        list.names = names.toArray(new Handle[names.size()]);
        return list;
    }
    
    /**
     * Loads the entries for all members of the specified gang.
     */
    protected ArrayList<GangMemberEntry> loadGangMembers (int gangId)
        throws PersistenceException
    {
        final ArrayList<GangMemberEntry> list = new ArrayList<GangMemberEntry>();
        final String query = "select HANDLE, GANG_MEMBERS.PLAYER_ID, RANK, " +
            "JOINED, NOTORIETY from GANG_MEMBERS, PLAYERS " +
            "where GANG_MEMBERS.PLAYER_ID = PLAYERS.PLAYER_ID and GANG_ID = " + gangId;
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        list.add(new GangMemberEntry(
                            new Handle(rs.getString(1)), rs.getInt(2),
                            rs.getByte(3), rs.getTimestamp(4), rs.getInt(5)));
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
            "STATEMENT TEXT NOT NULL",
            "URL VARCHAR(255) NOT NULL",
            "NOTORIETY INTEGER NOT NULL",
            "SCRIP INTEGER NOT NULL",
            "COINS INTEGER NOT NULL",
            "BRAND BLOB",
            "PRIMARY KEY (GANG_ID)",
            "UNIQUE (NAME)",
        }, "");

        // TEMP: add the statement and url columns, drop outfit
        JDBCUtil.addColumn(conn, "GANGS", "STATEMENT", "TEXT NOT NULL", "FOUNDED");
        JDBCUtil.addColumn(conn, "GANGS", "URL", "VARCHAR(255) NOT NULL", "STATEMENT");
        JDBCUtil.dropColumn(conn, "GANGS", "OUTFIT");
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
        
        JDBCUtil.createTableIfMissing(conn, "GANG_OUTFITS", new String[] {
            "GANG_ID INTEGER NOT NULL",
            "ARTICLE VARCHAR(64) NOT NULL",
            "ZATIONS INTEGER NOT NULL",
            "INDEX (GANG_ID)",
        }, "");
    }

    @Override // documentation inherited
    protected void createTables ()
    {
        _gtable = new Table<GangRecord>(GangRecord.class, "GANGS", "GANG_ID", true);
        _mtable = new Table<GangMemberRecord>(
            GangMemberRecord.class, "GANG_MEMBERS", "PLAYER_ID", true);
        _htable = new Table<GangHistoryRecord>(
            GangHistoryRecord.class, "GANG_HISTORY", "ENTRY_ID", true); 
        _otable = new Table<GangOutfitRecord>(
            GangOutfitRecord.class, "GANG_OUTFITS", "GANG_ID", true);
    }

    protected Table<GangRecord> _gtable;
    protected Table<GangMemberRecord> _mtable;
    protected Table<GangHistoryRecord> _htable;
    protected Table<GangOutfitRecord> _otable;
    protected FieldMask _gangIdMask, _playerIdMask;
}
