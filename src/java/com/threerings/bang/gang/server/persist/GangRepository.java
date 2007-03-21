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

import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.PlayerRecord;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangEntry;
import com.threerings.bang.gang.data.GangInfo;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HistoryEntry;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.gang.data.TopRankedGangList;
import com.threerings.bang.gang.util.GangUtil;

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
        _normalizedMask = _gtable.getFieldMask();
        _normalizedMask.setModified("normalized");
        _playerIdMask = _mtable.getFieldMask();
        _playerIdMask.setModified("playerId");
        _buckleMask = _gtable.getFieldMask();
        _buckleMask.setModified("buckle");
        _buckleMask.setModified("bucklePrint");
    }

    /**
     * Loads directory entries for all active gangs.
     */
    public ArrayList<GangEntry> loadGangs ()
        throws PersistenceException
    {
        final ArrayList<GangEntry> list = new ArrayList<GangEntry>();
        final String query = "select NAME, LAST_PLAYED from GANGS where LAST_PLAYED > " +
            STALE_DATE;
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        list.add(new GangEntry(new Handle(rs.getString(1)), rs.getTimestamp(2)));
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
     * the gang record; if false, only load what's necessary to populate a member's wanted poster
     */
    public GangRecord loadGang (int gangId, boolean all)
        throws PersistenceException
    {
        GangRecord grec = loadByExample(_gtable, new GangRecord(gangId), _gangIdMask);
        if (grec == null || !all) {
            return grec;
        }

        // load the gang inventory
        grec.inventory = BangServer.itemrepo.loadItems(gangId, true);

        // load the members
        grec.members = loadGangMembers(gangId);

        // load the coin count
        grec.coins = BangServer.coinmgr.getCoinRepository().getCoinCount(
            grec.getCoinAccount());

        // load the outfit
        ArrayList<GangOutfitRecord> recs = loadAll(_otable, "where GANG_ID = " + gangId);
        grec.outfit = new OutfitArticle[recs.size()];
        for (int ii = 0; ii < grec.outfit.length; ii++) {
            GangOutfitRecord rec = recs.get(ii);
            grec.outfit[ii] = new OutfitArticle(rec.article, rec.zations);
        }

        // load the senior leader's avatar
        GangMemberEntry leader = GangUtil.getSeniorLeader(grec.members);
        grec.avatar = (leader == null) ?
            null : BangServer.lookrepo.loadSnapshot(leader.playerId);

        return grec;
    }

    /**
     * Loads up the gang record associated with the specified name.  Returns null if no matching
     * record could be found.  This methods loads only the necessary information to populate a
     * {@link GangInfo} object.
     */
    public GangRecord loadGang (Handle name)
        throws PersistenceException
    {
        GangRecord grec = loadByExample(_gtable, new GangRecord(name), _normalizedMask);
        if (grec != null) {
            // load members, avatar; skip inventory, coins, outfit
            grec.members = loadGangMembers(grec.gangId);
            GangMemberEntry leader = GangUtil.getSeniorLeader(grec.members);
            grec.avatar = (leader == null) ?
                null : BangServer.lookrepo.loadSnapshot(leader.playerId);
        }
        return grec;
    }

    /**
     * Insert a new gang record into the repository and assigns a unique gang id in the process.
     * The {@link GangRecord#founded} and {@link GangRecord#lastPlayed} fields will be filled in
     * by this method if they are not already.
     *
     * @return true if the gang was successfully inserted, false if there was already a gang with
     * the desired name
     */
    public boolean insertGang (final GangRecord gang)
        throws PersistenceException
    {
        // make sure a gang with the specified name doesn't already exist
        if (loadByExample(_gtable, gang, _normalizedMask) != null) {
            return false;
        }
        if (gang.founded == null) {
            gang.founded = new Timestamp(System.currentTimeMillis());
        }
        if (gang.lastPlayed == null) {
            gang.lastPlayed = gang.founded;
        }
        gang.gangId = insert(_gtable, gang);
        return true;
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
     * Updates a gang's cached weight class.
     */
    public void updateWeightClass (int gangId, byte weightClass)
        throws PersistenceException
    {
        checkedUpdate("update GANGS set WEIGHT_CLASS = " + weightClass +
            " where GANG_ID = " + gangId, 1);
    }

    /**
     * Updates a gang's buckle.
     *
     * @param buckle an array containing the item ids of the buckle parts used, in order.
     * @param print the buckle fingerprint.
     */
    public void updateBuckle (int gangId, int[] buckle, int[] print)
        throws PersistenceException
    {
        GangRecord grec = new GangRecord(gangId);
        grec.setBuckle(buckle, print);
        if (update(_gtable, grec, _buckleMask) != 1) {
            throw new PersistenceException("Failed to update buckle [gangId=" + gangId + "].");
        }
    }

    /**
     * Updates a gang's configured outfit.
     */
    public void updateOutfit (int gangId, OutfitArticle[] outfit)
        throws PersistenceException
    {
        // delete the existing outfit
        update("delete from GANG_OUTFITS where GANG_ID = " + gangId);

        // add the new
        for (OutfitArticle oart : outfit) {
            insert(_otable, new GangOutfitRecord(gangId, oart.article, oart.zations));
        }
    }

    /**
     * Adds scrip to a gang's coffers.
     */
    public void grantScrip (int gangId, int scrip)
        throws PersistenceException
    {
        checkedUpdate("update GANGS set SCRIP = SCRIP + " + scrip +
                      " where GANG_ID = " + gangId, 1);
    }

    /**
     * Subtracts scrip from a gang's coffers.
     */
    public void spendScrip (int gangId, int scrip)
        throws PersistenceException
    {
        checkedUpdate("update GANGS set SCRIP = SCRIP - " + scrip +
                      " where GANG_ID = " + gangId, 1);
    }

    /**
     * Adds aces to the gang's coffers.
     */
    public void grantAces (int gangId, int aces)
        throws PersistenceException
    {
        checkedUpdate("update GANGS set ACES = ACES + " + aces +
                      " where GANG_ID = " + gangId, 1);
    }

    /**
     * Subtracts aces from the gang's coffers.
     */
    public void spendAces (int gangId, int aces)
        throws PersistenceException
    {
        checkedUpdate("update GANGS set ACES = ACES - " + aces +
                      " where GANG_ID = " + gangId, 1);
    }

    /**
     * Adds notoriety points to the gang and user records.
     */
    public void addNotoriety (int gangId, int playerId, int points)
        throws PersistenceException
    {
        checkedUpdate("update GANGS set NOTORIETY = NOTORIETY + " + points +
                      ", LAST_PLAYED = NOW() where GANG_ID = " + gangId, 1);
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
    public String deleteInvite (
        final int gangId, final int maxMembers, final int playerId, final boolean accepted)
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
                    if (rs.next() && rs.getInt(1) >= maxMembers - 1) {
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
     * Loads the top-ranked gangs in terms of notoriety for one weight class.
     */
    public TopRankedGangList loadTopRankedByNotoriety (byte weightClass, int count)
        throws PersistenceException
    {
        final String query = "select GANG_ID, NAME from GANGS where WEIGHT_CLASS = " +
            weightClass + " and LAST_PLAYED > " + STALE_DATE +
            " order by NOTORIETY desc limit " + count;

        final TopRankedGangList list = new TopRankedGangList();
        list.criterion = MessageBundle.compose("m.top_notoriety",
            MessageBundle.qualify(GangCodes.GANG_MSGS, "m.weight_class." + weightClass));

        final ArrayList<Handle> names = new ArrayList<Handle>();
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // load the names and remember the top gang's id
                    ResultSet rs = stmt.executeQuery(query);
                    if (!rs.next()) {
                        return null;
                    }
                    int topGangId = rs.getInt(1);
                    do {
                        names.add(new Handle(rs.getString(2)));
                    } while (rs.next());

                    // load the top gang's buckle print
                    rs = stmt.executeQuery("select BUCKLE_PRINT from GANGS where GANG_ID = " +
                        topGangId);
                    if (rs.next()) {
                        list.topDogBuckle = new BuckleInfo(GangRecord.decode(rs.getBytes(1)));
                    }
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
        if (names.isEmpty()) {
            return null;
        }
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
            "JOINED, NOTORIETY, LAST_SESSION from GANG_MEMBERS, PLAYERS " +
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
                            rs.getByte(3), rs.getTimestamp(4), rs.getInt(5),
                            rs.getTimestamp(6)));
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
            "NORMALIZED VARCHAR(64) NOT NULL UNIQUE",
            "FOUNDED DATETIME NOT NULL",
            "STATEMENT TEXT NOT NULL",
            "URL VARCHAR(255) NOT NULL",
            "WEIGHT_CLASS TINYINT NOT NULL",
            "NOTORIETY INTEGER NOT NULL",
            "LAST_PLAYED DATETIME NOT NULL",
            "SCRIP INTEGER NOT NULL",
            "ACES INTEGER NOT NULL",
            "BUCKLE BLOB NOT NULL",
            "BUCKLE_PRINT BLOB NOT NULL",
            "PRIMARY KEY (GANG_ID)",
        }, "");

        // TEMP: add aces, weight class, buckle print columns
        JDBCUtil.addColumn(conn, "GANGS", "ACES", "INTEGER NOT NULL", "SCRIP");
        JDBCUtil.addColumn(conn, "GANGS", "WEIGHT_CLASS", "TINYINT NOT NULL", "URL");
        JDBCUtil.addColumn(conn, "GANGS", "BUCKLE_PRINT", "BLOB NOT NULL", "BUCKLE");
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
    protected FieldMask _gangIdMask, _normalizedMask, _playerIdMask, _buckleMask;

    /** The cutoff after which a gang is considered inactive and is no longer
     * considered when calculating top scores. */
    protected static final String STALE_DATE =
        "DATE_SUB(NOW(), INTERVAL 2 WEEK)";
}
