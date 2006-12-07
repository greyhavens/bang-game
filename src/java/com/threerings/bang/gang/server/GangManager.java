//
// $Id$

package com.threerings.bang.gang.server;

import java.util.ArrayList;
import java.util.Date;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;

import com.samskivert.util.Collections;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.server.SpeakDispatcher;
import com.threerings.crowd.chat.server.SpeakProvider;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Notification;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.PosterInfo;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.PardnerEntryUpdater;
import com.threerings.bang.server.persist.FinancialAction;
import com.threerings.bang.server.persist.PlayerRecord;

import com.threerings.bang.saloon.server.SaloonManager;

import com.threerings.bang.gang.client.GangService;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangInvite;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HistoryEntry;
import com.threerings.bang.gang.server.persist.GangRepository;
import com.threerings.bang.gang.server.persist.GangRepository.GangRecord;
import com.threerings.bang.gang.server.persist.GangRepository.InviteRecord;
import com.threerings.bang.gang.server.persist.GangRepository.MemberRecord;

import static com.threerings.bang.Log.*;

/**
 * Handles gang-related functionality.
 */
public class GangManager
    implements GangProvider, SpeakProvider.SpeakerValidator, GangCodes
{
    /**
     * Initializes the gang manager and registers its invocation service.
     */
    public void init (ConnectionProvider conprov)
        throws PersistenceException
    {
        _gangrepo = new GangRepository(conprov);

        // register ourselves as the provider of the (bootstrap) GangService
        BangServer.invmgr.registerDispatcher(new GangDispatcher(this), true);
    }

    // documentation inherited from GangProvider
    public void inviteMember (
        ClientObject caller, final Handle handle, final String message,
        final GangService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're in a gang and can recruit
        final PlayerObject player = (PlayerObject)caller;
        if (!player.canRecruit()) {
            log.warning("Player not qualified to recruit gang members [who=" +
                player.who() + ", gangId=" + player.gangId + ", rank=" +
                player.gangRank + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // make sure it's not the player himself, that it's not already
        // a member, and that the player is under the limit
        if (player.handle.equals(handle)) {
            throw new InvocationException("e.invite_self");   
        }
        final GangObject gangobj = getGangObject(player.gangId);
        if (gangobj.members.containsKey(handle)) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.already_member_this", handle));
                
        } else if (gangobj.members.size() >= MAX_MEMBERS) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.too_many_members", String.valueOf(MAX_MEMBERS)));
        }
        
        // store the invitation in the database
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _error = _gangrepo.insertInvite(player.playerId, player.gangId, handle, message);
                if (_error == null) {
                    _gangrepo.insertHistoryEntry(player.gangId,
                        MessageBundle.tcompose("m.invited_entry", player.handle, handle));
                }
            }
            public void handleSuccess () {
                if (_error != null) {
                    listener.requestFailed(_error);
                    return;
                }
                PlayerObject invitee = (PlayerObject)BangServer.lookupBody(handle);
                if (invitee != null) {
                    sendGangInvite(invitee, player.handle, player.gangId, gangobj.name, message);
                }
                listener.requestProcessed();
            }
            protected String _error;
        });
    }
    
    /**
     * Loads the specified player's gang information.  This is called on the invoker thread when
     * resolving clients, and should be followed by a call to {@link #resolveGangObject} or
     * {@link #releaseGangObject} on the omgr thread.
     */
    public void loadGangData (PlayerObject player)
        throws PersistenceException
    {
        MemberRecord mrec = _gangrepo.loadMember(player.playerId);
        if (mrec != null) {
            player.gangId = mrec.gangId;
            player.gangRank = mrec.rank;
            player.joinedGang = mrec.joined.getTime();
            stashGangObject(player.gangId);
        } else {
            loadGangInvites(player);
        }
    }
    
    /**
     * Fetches the gang object referenced earlier on the invoker thread by
     * {@link #stashGangObject}, registering it if necessary.  Called when
     * the player client is successfully resolved.
     */
    public void resolveGangObject (PlayerObject user)
    {
        GangObject gangobj = getGangObject(user.gangId);
        gangobj.resolving--;
        new GangMemberEntryUpdater(user, gangobj).updateEntries();
        user.gangOid = gangobj.getOid();
    }
    
    /**
     * Releases the reference created earlier by {@link #stashGangObject}.
     * Called when an error occurs in client resolution.
     */ 
    public void releaseGangObject (int gangId)
    {
        GangObject gangobj = _gangs.get(gangId);
        gangobj.resolving--;
        maybeDestroyGangObject(gangobj);
    }
    
    /**
     * Populates the gang-related poster fields for an online player.
     */
    public void populatePosterInfo (PosterInfo info, PlayerObject player)
    {
        if (player.gangOid <= 0) {
            return;
        }
        GangObject gangobj = (GangObject)BangServer.omgr.getObject(player.gangOid);
        if (gangobj == null) {
            log.warning("Missing gang object for player [who=" + player.who() +
                ", oid=" + player.gangOid + "].");
            return;
        }
        info.gang = gangobj.name;
        info.rank = getPosterRank(player.gangRank);
    }
    
    /**
     * Populates the gang-related poster fields for an offline player (from the invoker thread).
     */
    public void populatePosterInfo (PosterInfo info, PlayerRecord player)
        throws PersistenceException
    {
        MemberRecord mrec = _gangrepo.loadMember(player.playerId);
        if (mrec != null) {
            info.gang = _gangrepo.loadGang(mrec.gangId, false).getName();
            info.rank = getPosterRank(mrec.rank);
        }
    }
    
    /**
     * Returns the name of the specified gang.  This is run from the invoker thread.
     */
    public Handle getGangName (int gangId)
        throws PersistenceException
    {
        // first try the loaded gangs
        synchronized (_gangs) {
            GangObject gangobj = _gangs.get(gangId);
            if (gangobj != null) {
                return gangobj.name;
            }
        }
        
        // then hit the database
        return _gangrepo.loadGang(gangId, false).getName();
    }
    
    /**
     * Processes a request to form a gang.  It is assumed that the player does
     * not already belong to a gang and that the provided name is valid.
     */
    public void formGang (
        final PlayerObject user, final Handle name,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        new FinancialAction(user, FORM_GANG_SCRIP_COST, FORM_GANG_COIN_COST) {
            protected int getCoinType () {
                return CoinTransaction.GANG_CREATION;
            }
            protected String getCoinDescrip () {
                return MessageBundle.tcompose("m.gang_creation", name);
            }
            protected String persistentAction () {
                try {
                    _gangrepo.insertGang(_grec);
                    _gangrepo.insertMember(
                        _mrec = new MemberRecord(user.playerId, _grec.gangId, LEADER_RANK));
                    _gangrepo.insertHistoryEntry(_grec.gangId,
                        MessageBundle.tcompose("m.founded_entry", user.handle));
                    return null;
                } catch (PersistenceException e) {
                    return INTERNAL_ERROR;
                }
            }
            protected void rollbackPersistentAction ()
                throws PersistenceException {
                // deleting the gang also deletes its history
                _gangrepo.deleteGang(_grec.gangId);
                _gangrepo.deleteMember(user.playerId);
            }
            protected void actionCompleted () {
                log.info("Formed new gang [who=" + user.who() + ", name=" +
                    name + ", gangId=" + _grec.gangId + "].");
                if (!user.isActive()) {
                    return; // he bailed; no point in continuing
                }
                GangObject gangobj = _grec.createGangObject();
                _gangs.put(_grec.gangId, gangobj);
                initGangObject(gangobj);
                try {
                    user.startTransaction();
                    user.setGangId(_grec.gangId);
                    user.setGangRank(LEADER_RANK);
                    user.setJoinedGang(_mrec.joined.getTime());
                    gangobj.addToMembers(
                        new GangMemberEntryUpdater(user, gangobj).gmentry);
                    user.setGangOid(gangobj.getOid());
                } finally {
                    user.commitTransaction();
                }
                listener.requestProcessed();
            }
            protected void actionFailed () {
                listener.requestFailed(INTERNAL_ERROR);
            }
            protected GangRecord _grec = new GangRecord(name.toString());
            protected MemberRecord _mrec;
        }.start();
    }
    
    /**
     * Processes a request to add to the gang's coffers.  It is assumed that
     * the player belongs to a gang and that the amounts are valid.
     */
    public void addToCoffers (
        final PlayerObject user, final int scrip, final int coins,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        new FinancialAction(user, scrip, coins) {
            protected int getCoinType () {
                return CoinTransaction.GANG_DONATION;
            }
            protected String getCoinDescrip () {
                return "m.gang_donation";
            }
            protected String persistentAction () {
                try {
                    _gangrepo.addToCoffers(user.gangId, scrip, coins);
                    _entryId = _gangrepo.insertHistoryEntry(user.gangId,
                        MessageBundle.compose(
                            "m.donation_entry",
                            MessageBundle.taint(user.handle),
                            getMoneyDesc(scrip, coins)));
                    return null;   
                } catch (PersistenceException e) {
                    return INTERNAL_ERROR;
                }
            }
            protected void rollbackPersistentAction ()
                throws PersistenceException {
                _gangrepo.addToCoffers(user.gangId, -scrip, -coins);
                if (_entryId > 0) {
                    _gangrepo.deleteHistoryEntry(_entryId);
                }
            }
            protected void actionCompleted () {
                log.info("Added to gang coffers [who=" + user.who() +
                    ", gangId=" + user.gangId + ", scrip=" + scrip +
                    ", coins=" + coins + "].");
                GangObject gangobj = getGangObject(user.gangId);
                if (gangobj == null) {
                    return; // adder bailed
                }
                try {
                    gangobj.startTransaction();
                    gangobj.setScrip(gangobj.scrip + scrip);
                    gangobj.setCoins(gangobj.coins + coins);
                } finally {
                    gangobj.commitTransaction();
                }
                listener.requestProcessed();
            }
            protected void actionFailed () {
                listener.requestFailed(INTERNAL_ERROR);
            }
            protected int _entryId;
        }.start();
    }
    
    /**
     * Processes a request to remove a member from a gang.  The entry provided
     * is assumed to describe a member of the specified gang.  At least one
     * member of the gang must be online.  When the last member is removed, the
     * gang itself is deleted.
     *
     * @param remover the leader responsible for kicking the member out, or
     * <code>null</code> if the member left voluntarily
     */
    public void removeFromGang (
        final int gangId, final int playerId, final Handle handle,
        final Handle remover, final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        GangObject gangobj = getGangObject(gangId);
        if (gangobj == null) {
            log.warning("Missing gang object for removal [gangId=" +
                gangId + ", playerId=" + playerId + ", handle=" +
                handle + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // determine whether to delete the gang
        final boolean delete = (gangobj.members.size() == 1);
        
        // gangs cannot be left without a leader; if we are removing the last
        // leader, we must promote the most senior non-leader
        GangMemberEntry removal = gangobj.members.get(handle);
        if (removal.rank == LEADER_RANK && remover == null && !delete) {
            boolean leaderless = true;
            GangMemberEntry senior = null;
            for (GangMemberEntry member : gangobj.members) {
                if (member == removal) {
                    continue;
                } else if (member.rank == LEADER_RANK) {
                    leaderless = false;
                    break;
                } else if (senior == null || member.joined < senior.joined) {
                    senior = member;
                }
            }
            if (leaderless) {
                changeMemberRank(
                    gangId, senior.playerId, senior.handle, null, senior.rank, LEADER_RANK,
                    new InvocationService.ConfirmListener() {
                        public void requestProcessed () {
                            continueRemovingFromGang(
                                gangId, playerId, handle, null, false, listener);
                        }
                        public void requestFailed (String cause) {
                            listener.requestFailed(cause);
                        }
                    });
                return;
            }
        }
        
        // continue the process
        continueRemovingFromGang(gangId, playerId, handle, remover, delete, listener);
    }
    
    /**
     * Processes a request to change a member's rank.  The entry provided is
     * assumed to describe a member of the specified gang.
     *
     * @param changer the leader responsible for changing the member's rank, or <code>null</code>
     * if the rank was changed by default
     */
    public void changeMemberRank (
        final int gangId, final int playerId, final Handle handle, final Handle changer,
        final byte orank, final byte nrank, final InvocationService.ConfirmListener listener)
    {
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _gangrepo.updateRank(playerId, nrank);
                _gangrepo.insertHistoryEntry(gangId, (changer == null) ?
                    MessageBundle.tcompose("m.auto_promotion_entry", handle) :
                    MessageBundle.compose(
                        (orank < nrank) ? "m.promotion_entry" : "m.demotion_entry",
                        MessageBundle.taint(changer),
                        MessageBundle.taint(handle),
                        MessageBundle.qualify(GANG_MSGS, XLATE_RANKS[nrank])));
            }
            public void handleSuccess () {
                log.info("Changed member rank [gangId=" + gangId +
                    ", playerId=" + playerId + ", handle=" + handle +
                    ", rank=" + nrank + "].");
                PlayerObject plobj =
                    (PlayerObject)BangServer.lookupBody(handle);
                if (plobj != null) {
                    plobj.setGangRank(nrank);
                }
                GangObject gangobj = getGangObject(gangId);
                if (gangobj != null) {
                    GangMemberEntry entry = gangobj.members.get(handle);
                    entry.rank = nrank;
                    gangobj.updateMembers(entry);
                }
                listener.requestProcessed();
            } 
        });
    }
    
    /**
     * Fetches a batch of history entries for the specified gang at the requested offset.
     */
    public void getHistoryEntries (
        final int gangId, final int offset, final int count,
        final InvocationService.ResultListener listener)
    {
         BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _entries = _gangrepo.loadHistoryEntries(gangId, offset, count);
            }
            public void handleSuccess () {
                listener.requestProcessed(
                    _entries.toArray(new HistoryEntry[_entries.size()]));
            }
            protected ArrayList<HistoryEntry> _entries;
        });
    }
    
    // documentation inherited from interface SpeakProvider.SpeakerValidator
    public boolean isValidSpeaker (
        DObject speakObj, ClientObject speaker, byte mode)
    {
        GangObject gangobj = (GangObject)speakObj;
        PlayerObject user = (PlayerObject)speaker;
        return gangobj.members.containsKey(user.handle);
    }
    
    /**
     * Returns a translatable string describing the identified amounts (at least one of which must
     * be nonzero).
     */
    protected String getMoneyDesc (int scrip, int coins)
    {
        String sdesc = null, cdesc = null;
        if (scrip > 0) {
            sdesc = MessageBundle.tcompose("m.scrip", String.valueOf(scrip));
            if (coins == 0) {
                return sdesc;
            }
        }
        if (coins > 0) {
            cdesc = MessageBundle.tcompose("m.coins", coins);
            if (scrip == 0) {
                return cdesc;
            }
        }
        return MessageBundle.compose("m.times_2", cdesc, sdesc);
    }
    
    /**
     * Ensures that the gang data for the identified gang is loaded from the
     * database.  
     */
    protected void stashGangObject (int gangId)
        throws PersistenceException
    {
        // first, determine if the object is already loaded
        GangObject gangobj;
        synchronized (_gangs) {
            if ((gangobj = _gangs.get(gangId)) != null) {
                gangobj.resolving++;
                return;
            }
        }
        
        // if not, we must load it
        gangobj = _gangrepo.loadGang(gangId, true).createGangObject();
        gangobj.resolving++;
        _gangs.put(gangId, gangobj);
    }
    
    /**
     * Loads the user's gang invitations, if any, on the invoker thread.
     */
    protected void loadGangInvites (PlayerObject user)
        throws PersistenceException
    {
        ArrayList<InviteRecord> records =
            _gangrepo.getInviteRecords(user.playerId);
        for (InviteRecord record : records) {
            sendGangInvite(user, record.inviter, record.gangId, record.name, record.message);
        }
    }
    
    /**
     * Sends an invitation to join a gang.
     */
    protected void sendGangInvite (
        final PlayerObject user, final Handle inviter, final int gangId,
        final Handle name, String message)
    {
        user.addToNotifications(new GangInvite(inviter, name, message,
            new GangInvite.ResponseHandler() {
            public void handleResponse (int resp, InvocationService.ConfirmListener listener) {
                handleInviteResponse(
                    user, inviter, gangId, name, (resp == GangInvite.ACCEPT), listener);
            }
        }));
    }
    
    /**
     * Processes the response to a gang invitation.
     */
    protected void handleInviteResponse (
        final PlayerObject user, final Handle inviter, final int gangId, final Handle name,
        final boolean accept, final InvocationService.ConfirmListener listener)
    {
        // update the database
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _error = _gangrepo.deleteInvite(gangId, user.playerId, accept);
                if (_error == null && accept) {
                    // stash the gang object before we add the player to ensure that the
                    // dset does not contain an entry for the new member
                    stashGangObject(gangId);
                    _gangrepo.insertMember(
                        _mrec = new MemberRecord(user.playerId, gangId, MEMBER_RANK));
                    _gangrepo.insertHistoryEntry(gangId,
                        MessageBundle.tcompose("m.joined_entry", user.handle));
                }
            }
            public void handleSuccess () {
                if (_error != null) {
                    listener.requestFailed(_error);
                } else {
                    handleInviteSuccess(user, inviter, gangId, name, accept, _mrec, listener);
                }
            }
            public String getFailureMessage () {
                return "Failed to respond to gang invite [who=" + user.who() +
                    ", gangId=" + gangId + ", accept=" + accept + "]";
            }
            protected MemberRecord _mrec;
            protected String _error;
        });
    }
    
    /**
     * Handles the omgr portion of the invite processing, once the persistent
     * part has successfully completed.
     */
    protected void handleInviteSuccess (
        PlayerObject user, Handle inviter, int gangId, Handle name, boolean accept,
        MemberRecord mrec, InvocationService.ConfirmListener listener)
    {
        PlayerObject invobj = (PlayerObject)BangServer.lookupBody(inviter);
        if (invobj != null) {
            SpeakProvider.sendInfo(invobj, GANG_MSGS,
                MessageBundle.tcompose(
                    accept ? "m.member_accepted" : "m.member_rejected",
                    user.handle, name));
        }
        
        if (!accept) { // nothing to do
            listener.requestProcessed();
            return;
            
        } else if (!user.isActive()) { // add offline entry
            releaseGangObject(gangId);
            GangObject gangobj = _gangs.get(gangId);
            if (gangobj != null) {
                gangobj.addToMembers(new GangMemberEntry(
                    user.playerId, user.handle, MEMBER_RANK, mrec.joined, mrec.joined));
            }
            return;
        }
        GangObject gangobj = getGangObject(gangId);
        gangobj.resolving--;
        ArrayList<Comparable> keys = new ArrayList<Comparable>();
        for (Notification notification : user.notifications) {
            // we want to remove all gang invites *except* for the one we're
            // answering, because that one will be removed by the caller
            if (notification instanceof GangInvite &&
                !((GangInvite)notification).gang.equals(name)) {
                keys.add(notification.getKey());
            }
        }
        try {
            user.startTransaction();
            user.setGangId(gangId);
            user.setGangRank(MEMBER_RANK);
            user.setJoinedGang(mrec.joined.getTime());
            gangobj.addToMembers(
                new GangMemberEntryUpdater(user, gangobj).gmentry);
            user.setGangOid(gangobj.getOid());
            for (Comparable key : keys) {
                user.removeFromNotifications(key);
            }
        } finally {
            user.commitTransaction();
        }
        listener.requestProcessed();
    }
    
    /**
     * Continues the process of removing a member from a gang, perhaps after having promoted
     * a player to avoid leaving a gang leaderless.
     */
    protected void continueRemovingFromGang (
        final int gangId, final int playerId, final Handle handle, final Handle remover,
        final boolean delete, final InvocationService.ConfirmListener listener)
    {
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _gangrepo.deleteMember(playerId);
                if (delete) {
                    _gangrepo.deleteGang(gangId);
                } else {
                    _gangrepo.insertHistoryEntry(gangId, (remover == null) ?
                        MessageBundle.tcompose("m.left_entry", handle) :
                        MessageBundle.tcompose("m.expelled_entry", remover, handle));
                }
            }
            public void handleSuccess () {
                log.info("Removed member from gang [gangId=" + gangId +
                    ", playerId=" + playerId + ", handle=" + handle +
                    ", delete=" + delete + "].");
                // the order is important here; the updater will remove itself
                // when the gang id is cleared and destroy the gang object if
                // there are no more members online
                GangObject gangobj = getGangObject(gangId);
                if (gangobj != null) {
                    gangobj.removeFromMembers(handle);    
                }
                PlayerObject plobj =
                    (PlayerObject)BangServer.lookupBody(handle);
                if (plobj != null) {
                    try {
                        plobj.startTransaction();
                        plobj.setGangId(0);
                        plobj.setGangOid(0);
                    } finally {
                        plobj.commitTransaction();
                    }
                } 
                listener.requestProcessed();
            } 
        });
    }
    
    /**
     * Retrieves the identified gang object, initializing it if necessary.
     */
    protected GangObject getGangObject (int gangId)
    {
        GangObject gangobj = _gangs.get(gangId);
        if (gangobj != null && !gangobj.isActive()) {
            initGangObject(gangobj);
        }
        return gangobj;
    }
    
    /**
     * Initializes and registers a previously created and mapped gang object.
     */
    protected void initGangObject (final GangObject gangobj)
    {
        gangobj.speakService =
            (SpeakMarshaller)BangServer.invmgr.registerDispatcher(
                new SpeakDispatcher(new SpeakProvider(gangobj, this)), false);
        (gangobj.rankval = new Interval(BangServer.omgr) {
            public void expired () {
                SaloonManager.refreshTopRanked(
                    gangobj,
                    "GANG_MEMBERS",
                    "RATINGS.PLAYER_ID = GANG_MEMBERS.PLAYER_ID and GANG_ID = " + gangobj.gangId,
                    TOP_RANKED_LIST_SIZE);
            }
        }).schedule(1000L, RANK_REFRESH_INTERVAL);
        BangServer.omgr.registerObject(gangobj);
        log.info("Initialized gang object [gangId=" + gangobj.gangId +
            ", name=" + gangobj.name + "].");
    }
    
    /**
     * Unmaps and destroys the given gang object if there are no more members
     * online or in the process of resolution.
     */
    protected void maybeDestroyGangObject (GangObject gangobj)
    {
        synchronized (_gangs) {
            if (!gangobj.canBeDestroyed()) {
                return;
            }
            _gangs.remove(gangobj.gangId);
        }
        BangServer.invmgr.clearDispatcher(gangobj.speakService);
        gangobj.rankval.cancel();
        gangobj.destroy();
        log.info("Destroyed gang object [gangId=" + gangobj.gangId +
            ", name=" + gangobj.name + "].");
    }
    
    /**
     * Converts an actual rank to a rank appropriate for display on a poster.
     */
    protected static byte getPosterRank (byte rank)
    {
        return (rank == RECRUITER_RANK) ? MEMBER_RANK : rank;
    }
    
    /** Updates gang member set entries as the player objects change. */
    protected class GangMemberEntryUpdater extends PardnerEntryUpdater
    {
        public GangMemberEntry gmentry;
        
        public GangMemberEntryUpdater (PlayerObject player, GangObject gangobj)
        {
            super(player);
            _gangobj = gangobj;
            gmentry = (GangMemberEntry)entry;
        }
        
        @Override // documentation inherited
        protected PardnerEntry createPardnerEntry (PlayerObject player)
        {
            return new GangMemberEntry(player);
        }
        
        @Override // documentation inherited
        public void attributeChanged (AttributeChangedEvent ace)
        {
            // remove the updater if the player leaves the gang
            super.attributeChanged(ace);
            if (ace.getName().equals(PlayerObject.GANG_ID) &&
                _player.gangId <= 0) {
                remove();
            }
        }
        
        @Override // documentation inherited
        public void updateEntries ()
        {
            _gangobj.updateMembers(gmentry);
        }
        
        @Override // documentation inherited
        protected boolean shouldRemove ()
        {
            // only remove when player object destroyed or removed from gang
            return (!_player.isActive() || _player.gangId <= 0);
        }
        
        @Override // documentation inherited
        protected void remove ()
        {
            // when the last member logs off, remove the dobj
            super.remove();
            maybeDestroyGangObject(_gangobj);
        }
        
        /** The gang object to update when the entry changes. */
        protected GangObject _gangobj;
    }
    
    /** The persistent store for gang data. */
    protected GangRepository _gangrepo;
    
    /** Maps gang ids to currently loaded gang objects. */
    protected IntMap<GangObject> _gangs =
        Collections.synchronizedIntMap(new HashIntMap<GangObject>());
    
    /** The frequency with which we update the top-ranked member lists. */
    protected static final long RANK_REFRESH_INTERVAL = 60 * 60 * 1000L;

    /** The size of the top-ranked member lists. */
    protected static final int TOP_RANKED_LIST_SIZE = 10;
}
