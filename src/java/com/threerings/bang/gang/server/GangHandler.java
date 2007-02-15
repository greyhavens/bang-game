//
// $Id$

package com.threerings.bang.gang.server;

import java.util.ArrayList;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.RepositoryUnit;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Interval;
import com.samskivert.util.ResultListener;
import com.samskivert.util.ResultListenerList;
import com.samskivert.util.Tuple;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.peer.data.NodeObject.Lock;
import com.threerings.presents.peer.util.PeerUtil;
import com.threerings.presents.server.ClientManager.ClientObserver;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.chat.server.SpeakProvider;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BangClientInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Notification;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangPeerManager.RemotePlayerObserver;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.BangServer.PlayerObserver;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.PeerFinancialAction;
import com.threerings.bang.server.persist.ProxyFinancialAction;

import com.threerings.bang.avatar.util.ArticleCatalog;

import com.threerings.bang.saloon.server.SaloonManager;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangInvite;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.GangPeerMarshaller;
import com.threerings.bang.gang.data.HistoryEntry;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.gang.server.persist.GangFinancialAction;
import com.threerings.bang.gang.server.persist.GangMemberRecord;
import com.threerings.bang.gang.server.persist.GangRecord;

import static com.threerings.bang.Log.*;

/**
 * Manages a single gang from resolution to destruction.
 */
public class GangHandler
    implements PlayerObserver, RemotePlayerObserver, SetListener, ObjectDeathListener,
        GangPeerProvider, GangCodes
{
    /**
     * Creates the handler and starts the process of resolving the specified gang.
     */
    public GangHandler (int gangId)
    {
        _gangId = gangId;

        // if not running in peer mode, we can skip to the database stage
        if (BangServer.peermgr == null) {
            loadFromDatabase();
            return;
        }

        // otherwise, we must acquire the peer lock, or find out who has it
        BangServer.peermgr.acquireLock(new Lock("gang", _gangId),
            new ResultListener<String>() {
                public void requestCompleted (String result) {
                    if (result.equals(ServerConfig.nodename)) {
                        loadFromDatabase();
                    } else {
                        subscribeToPeer(result);
                    }
                }
                public void requestFailed (Exception cause) {
                    initFailed(cause);
                }
            });
    }

    /**
     * Creates a handler for a newly created gang.
     */
    public GangHandler (GangRecord grec, PlayerObject creator)
    {
        _gangId = grec.gangId;

        // create the object immediately
        createGangObject(grec);

        // initialize the creator
        initPlayer(creator);
    }

    /**
     * Returns a reference to the gang's distributed object, if it has been resolved.
     */
    public GangObject getGangObject ()
    {
        return _gangobj;
    }

    /**
     * Asynchronously retrieves a reference to the gang object.  If the gang is in the process
     * of resolving, the listener will be notified when it is finished.
     */
    public void getGangObject (ResultListener<GangObject> listener)
    {
        if (_gangobj != null) {
            // any time the gang object is requested, it means we should cancel any effort to
            // unload; however, we need to reschedule the unload in case nothing else happens
            if (_client == null) {
                maybeCancelUnload();
            }
            listener.requestCompleted(_gangobj);
            if (_client == null) {
                maybeScheduleUnload();
            }
        } else {
            _listeners.add(listener);
        }
    }

    /**
     * Returns the object used to access the {@link GangPeerProvider} interface from server
     * entities, which will either be this handler or a proxy object that forwards requests
     * to a peer.
     */
    public GangPeerProvider getPeerProvider ()
    {
        return (_client == null) ? this : _proxy;
    }

    /**
     * Fetches a batch of history entries.  This need not be forwarded to the controlling peer
     * because it simply reads the entries from the database.
     */
    public void getHistoryEntries (
        final int offset, final int count, final InvocationService.ResultListener listener)
    {
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                _entries = BangServer.gangrepo.loadHistoryEntries(_gangId, offset, count);
            }
            public void handleSuccess () {
                listener.requestProcessed(_entries.toArray(new HistoryEntry[_entries.size()]));
            }
            protected ArrayList<HistoryEntry> _entries;
        });
    }

    /**
     * Transfers money from the user to the gang's coffers.  This must be coordinated between the
     * peer on which the user is logged in and the controlling peer.
     */
    public void addToCoffers (
        PlayerObject user, final int scrip, final int coins,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        new ProxyFinancialAction(user, scrip, coins) { {
                // because we're transferring rather than spending, we need to reserve even
                // for admins
                _scripCost = scrip;
                _coinCost = coins;
            }
            protected void forwardRequest () throws InvocationException {
                getPeerProvider().addToCoffers(
                    null, _user.handle, getCoinAccount(), scrip, coins, this);
            }
            protected void actionCompleted () {
                listener.requestProcessed();
            }
            protected void actionFailed (String cause) {
                listener.requestFailed(cause);
            }
        }.start();
    }

    @Override // documentation inherited
    public String toString ()
    {
        return (_gangobj == null ? "" : (_gangobj.name + " ")) + "(" + _gangId + ")";
    }

    // documentation inherited from interface PlayerObserver
    public void playerLoggedOn (PlayerObject user)
    {
        playerLocationChanged(user.handle, ServerConfig.townIndex);
    }

    // documentation inherited from interface PlayerObserver
    public void playerLoggedOff (PlayerObject user)
    {
        playerLocationChanged(user.handle, -1);
    }

    // documentation inherited from interface PlayerObserver
    public void playerChangedHandle (PlayerObject user, Handle oldHandle)
    {
        // TODO: handle this for remote members
        GangMemberEntry entry = _gangobj.members.get(oldHandle);
        if (entry != null) {
            _gangobj.startTransaction();
            try {
                _gangobj.removeFromMembers(oldHandle);
                entry = (GangMemberEntry)entry.clone();
                entry.handle = user.handle;
                _gangobj.addToMembers(entry);
            } finally {
                _gangobj.commitTransaction();
            }
        }
    }

    // documentation inherited from interface RemotePlayerObserver
    public void remotePlayerLoggedOn (int townIndex, BangClientInfo info)
    {
        playerLocationChanged(info.visibleName, townIndex);
    }

    // documentation inherited from interface RemotePlayerObserver
    public void remotePlayerLoggedOff (int townIndex, BangClientInfo info)
    {
        playerLocationChanged(info.visibleName, -1);
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (!event.getName().equals(GangObject.MEMBERS)) {
            return;
        }
        // set the user's gang fields and remove any outstanding gang invitations (except the one
        // for this gang, which will be removed by the response handler) if he's online
        GangMemberEntry entry = (GangMemberEntry)event.getEntry();
        PlayerObject user = BangServer.lookupPlayer(entry.handle);
        if (user != null) {
            ArrayList<GangInvite> invites = new ArrayList<GangInvite>();
            for (Notification note : user.notifications) {
                if (!(note instanceof GangInvite)) {
                    continue;
                }
                GangInvite invite = (GangInvite)note;
                if (!invite.gang.equals(_gangobj.name)) {
                    invites.add(invite);
                }
            }
            user.startTransaction();
            try {
                initPlayer(user, entry);
                for (GangInvite invite : invites) {
                    user.removeFromNotifications(invite.gang);
                }
            } finally {
                user.commitTransaction();
            }
        }
    }

    /**
     * Initializes the gang fields of the player object.
     */
    public void initPlayer (PlayerObject user)
    {
        GangMemberEntry entry = _gangobj.members.get(user.handle);
        if (entry != null) {
            initPlayer(user, entry);
        } else {
            log.warning("User not in gang [gang=" + this + ", who=" + user.who() + "].");
        }
    }

    /**
     * Initializes the gang fields of the player object based on his gang entry.
     */
    public void initPlayer (PlayerObject user, GangMemberEntry entry)
    {
        // set the gang fields of their player object
        user.startTransaction();
        try {
            user.setGangId(_gangId);
            user.setGangRank(entry.rank);
            user.setJoinedGang(entry.joined);
            user.setGangOid(_gangobj.getOid());
        } finally {
            user.commitTransaction();
        }

        // if they're the senior leader, start tracking their avatar
        updateAvatarUpdater(user);
    }

    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (!event.getName().equals(GangObject.MEMBERS)) {
            return;
        }
        // make sure we're tracking the right avatar
        updateAvatarUpdater(null);

        // clear the user's gang fields if he's online
        Handle handle = (Handle)event.getKey();
        PlayerObject user = BangServer.lookupPlayer(handle);
        if (user == null) {
            return;
        }
        user.startTransaction();
        try {
            user.setGangId(0);
            user.setGangOid(0);
        } finally {
            user.commitTransaction();
        }
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (!event.getName().equals(GangObject.MEMBERS)) {
            return;
        }
        // make sure we're tracking the right avatar
        updateAvatarUpdater(null);

        // update the user's gang fields if he's online
        GangMemberEntry entry = (GangMemberEntry)event.getEntry();
        PlayerObject user = BangServer.lookupPlayer(entry.handle);
        if (user == null || user.gangRank == entry.rank) {
            return;
        }
        user.setGangRank(entry.rank);
    }

    // documentation inherited from interface ObjectDeathListener
    public void objectDestroyed (ObjectDestroyedEvent event)
    {
        if (_client != null) {
            unsubscribeFromPeer();
        }
    }

    // documentation inherited from interface GangPeerProvider
    public void grantNotoriety (
        ClientObject caller, final Handle handle, final int points)
    {
        // make sure it comes from this server or a peer
        GangMemberEntry member = null;
        try {
            verifyLocalOrPeer(caller);
            member = verifyInGang(handle);
        } catch (InvocationException e) {
            return;
        }

        // update the database
        final GangMemberEntry entry = member;
        BangServer.invoker.postUnit(new RepositoryUnit() {
            public void invokePersist ()
                throws PersistenceException {
                BangServer.gangrepo.addNotoriety(_gangId, entry.playerId, points);
            }
            public void handleSuccess () {
                GangMemberEntry member = _gangobj.members.get(handle);
                _gangobj.startTransaction();
                try {
                    _gangobj.setNotoriety(_gangobj.notoriety + points);
                    _gangobj.setNotorietyRank(getNotorietyRank(_gangobj.notoriety));
                    if (member != null) {
                        member.notoriety += points;
                        _gangobj.updateMembers(member);
                    }
                } finally {
                    _gangobj.commitTransaction();
                }
            }
            public void handleFailure (Exception cause) {
                log.warning("Failed to grant notoriety [gang=" + GangHandler.this + ", handle=" +
                    handle + ", points=" + points + ", error=" + cause + "].");
            }
        });
    }

    // documentation inherited from interface GangPeerProvider
    public void setAvatar (ClientObject caller, AvatarInfo info)
    {
        // make sure it comes from this server or a peer
        try {
            verifyLocalOrPeer(caller);
        } catch (InvocationException e) {
            return;
        }

        // set it immediately
        _gangobj.setAvatar(info);
    }

    // documentation inherited from interface GangPeerProvider
    public void inviteMember (
        ClientObject caller, final Handle handle, final Handle target, final String message,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure they're in the gang and can recruit
        final GangMemberEntry entry = verifyInGang(handle);
        if (entry.rank < RECRUITER_RANK) {
            log.warning("Member cannot recruit [gang=" + this + ", member=" + entry + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure they're not already a member and that we're not at our limit
        if (_gangobj.members.containsKey(target)) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.already_member_this", target));
        } else if (_gangobj.members.size() >= MAX_MEMBERS) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.too_many_members", String.valueOf(MAX_MEMBERS)));
        }

        // store the invitation in the database
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _error = BangServer.gangrepo.insertInvite(
                    entry.playerId, _gangId, target, message);
                if (_error == null) {
                    BangServer.gangrepo.insertHistoryEntry(_gangId,
                        MessageBundle.tcompose("m.invited_entry", handle, target));
                }
            }
            public void handleSuccess () {
                if (_error != null) {
                    listener.requestFailed(_error);
                    return;
                }
                BangServer.gangmgr.sendGangInvite(target, handle, _gangId, _gangobj.name, message);
                listener.requestProcessed();
            }
            protected String _error;
        });
    }

    // documentation inherited from interface GangPeerProvider
    public void handleInviteResponse (
        ClientObject caller, final Handle handle, final int playerId, final Handle inviter,
        final boolean accept, final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // update the database
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                _error = BangServer.gangrepo.deleteInvite(_gangId, playerId, accept);
                if (_error == null && accept) {
                    _mrec = new GangMemberRecord(playerId, _gangId, MEMBER_RANK);
                    BangServer.gangrepo.insertMember(_mrec);
                    String hmsg = MessageBundle.tcompose("m.joined_entry", handle);
                    BangServer.gangrepo.insertHistoryEntry(_gangId, hmsg);
                }
            }
            public void handleSuccess () {
                if (_error != null) {
                    listener.requestFailed(_error);
                } else {
                    handleInviteSuccess(handle, playerId, inviter, _mrec, listener);
                }
            }
            public String getFailureMessage () {
                return "Failed to respond to gang invite [who=" + handle +
                    ", gang=" + this + ", accept=" + accept + "]";
            }
            protected GangMemberRecord _mrec;
            protected String _error;
        });
    }

    // documentation inherited from interface GangPeerProvider
    public void setStatement (
        ClientObject caller, Handle handle, final String statement,
        final String url, final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure it comes from a leader
        verifyIsLeader(handle);

        // post to the database
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                BangServer.gangrepo.updateStatement(_gangId, statement, url);
            }
            public void handleSuccess () {
                _gangobj.startTransaction();
                try {
                    _gangobj.setStatement(statement);
                    _gangobj.setUrl(url);
                } finally {
                    _gangobj.commitTransaction();
                }
                listener.requestProcessed();
            }
        });
    }

    // documentation inherited from interface GangPeerProvider
    public void addToCoffers (
        ClientObject caller, final Handle handle, String coinAccount,
        final int scrip, final int coins, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure they're in the gang
        final GangMemberEntry entry = verifyInGang(handle);

        // start the action
        new PeerFinancialAction(coinAccount, entry.playerId, scrip, coins, listener) {
            protected String persistentAction () {
                try {
                    BangServer.gangrepo.grantScrip(_gangId, scrip);
                    _entryId = BangServer.gangrepo.insertHistoryEntry(_gangId,
                        MessageBundle.compose(
                            "m.donation_entry",
                            MessageBundle.taint(handle),
                            getMoneyDesc(scrip, coins)));
                    return null;
                } catch (PersistenceException e) {
                    return INTERNAL_ERROR;
                }
            }
            protected boolean spendCoins (int reservationId)
                throws PersistenceException {
                return BangServer.coinmgr.getCoinRepository().transferCoins(
                    reservationId, _gangobj.getCoinAccount(), CoinTransaction.GANG_DONATION,
                    "m.gang_donation", "m.gang_donation");
            }
            protected void rollbackPersistentAction ()
                throws PersistenceException {
                BangServer.gangrepo.spendScrip(_gangId, scrip);
                if (_entryId > 0) {
                    BangServer.gangrepo.deleteHistoryEntry(_entryId);
                }
            }

            protected void actionCompleted () {
                log.info("Added to gang coffers [gang=" + this + ", member=" + handle +
                         ", scrip=" + scrip + ", coins=" + coins + "].");
                _gangobj.startTransaction();
                try {
                    _gangobj.setScrip(_gangobj.scrip + scrip);
                    _gangobj.setCoins(_gangobj.coins + coins);
                } finally {
                    _gangobj.commitTransaction();
                }
                super.actionCompleted();
            }

            protected int _entryId;
        }.start();
    }

    // documentation inherited from interface GangPeerProvider
    public void removeFromGang (
        ClientObject caller, final Handle handle, final Handle target,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure they can remove
        final GangMemberEntry entry = verifyCanChange(handle, target);

        // determine whether to delete the gang
        boolean delete = (_gangobj.members.size() == 1);

        // gangs cannot be left without a leader; if we are removing the last leader, we must
        // promote the most senior non-leader.  otherwise, if the most senior leader leaves,
        // we must note the next most senior leader in order to update their avatar
        GangMemberEntry senior = null;
        if (entry.rank == LEADER_RANK && handle == null && !delete) {
            boolean leaderless = true;
            GangMemberEntry msenior = null, lsenior = null;
            for (GangMemberEntry member : _gangobj.members) {
                if (member == entry) {
                    continue;
                } else if (member.rank == LEADER_RANK &&
                    (lsenior == null || member.joined < lsenior.joined)) {
                    lsenior = member;
                } else if (msenior == null || member.joined < msenior.joined) {
                    msenior = member;
                }
            }
            if (lsenior == null) {
                final GangMemberEntry fsenior = msenior;
                changeMemberRank(null, null, msenior.handle, LEADER_RANK,
                    new InvocationService.ConfirmListener() {
                        public void requestProcessed () {
                            continueRemovingFromGang(handle, entry, fsenior, false, listener);
                        }
                        public void requestFailed (String cause) {
                            listener.requestFailed(cause);
                        }
                    });
                return;

            } else if (entry == _gangobj.getSeniorLeader()) {
                senior = lsenior;
            }
        }

        // continue the process
        continueRemovingFromGang(handle, entry, senior, delete, listener);
    }

    // documentation inherited from interface GangPeerProvider
    public void changeMemberRank (
        ClientObject caller, final Handle handle, final Handle target, final byte rank,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure they can change it
        final GangMemberEntry entry = verifyCanChange(handle, target);

        // make sure it's not the same rank
        if (rank == entry.rank) {
            log.warning("Tried to change to same rank [handle=" + handle +
                ", target=" + entry + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // post to the database
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                BangServer.gangrepo.updateRank(entry.playerId, rank);
                BangServer.gangrepo.insertHistoryEntry(_gangId, (handle == null) ?
                    MessageBundle.tcompose("m.auto_promotion_entry", target) :
                    MessageBundle.compose(
                        (entry.rank < rank) ? "m.promotion_entry" : "m.demotion_entry",
                        MessageBundle.taint(handle),
                        MessageBundle.taint(target),
                        MessageBundle.qualify(GANG_MSGS, XLATE_RANKS[rank])));
            }
            public void handleSuccess () {
                GangMemberEntry member = _gangobj.members.get(target);
                member.rank = rank;
                _gangobj.updateMembers(member);
                listener.requestProcessed();
            }
        });
    }

    // documentation inherited from interface GangPeerProvider
    public void processOutfits (
        ClientObject caller, final Handle handle, final OutfitArticle[] outfit, final boolean buy,
        final boolean admin, final InvocationService.ResultListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure it comes from a leader
        verifyIsLeader(handle);

        // validate the outfit and get the corresponding articles
        // TODO: make sure gang has access to requested articles and colors
        final ArticleCatalog.Article[] catarts = new ArticleCatalog.Article[outfit.length];
        final Article[] articles = new Article[outfit.length];
        for (int ii = 0; ii < outfit.length; ii++) {
            OutfitArticle oart = outfit[ii];
            catarts[ii] = BangServer.alogic.getArticleCatalog().getArticle(oart.article);
            if (catarts[ii] == null) {
                log.warning("Invalid article requested for outfit [who=" + handle +
                    ", article=" + oart.article + "].");
                throw new InvocationException(INTERNAL_ERROR);
            }
            articles[ii] = BangServer.alogic.createArticle(-1, catarts[ii], oart.zations);
            if (articles[ii] == null) {
                // an error will have already been logged
                throw new InvocationException(INTERNAL_ERROR);
            }
        }

        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                // save the outfit as the gang's current
                BangServer.gangrepo.updateOutfit(_gangId, outfit);

                // find out who needs the articles and how much it will cost
                ArrayIntSet maleIds = new ArrayIntSet(), femaleIds = new ArrayIntSet();
                BangServer.gangrepo.loadMemberIds(_gangId, maleIds, femaleIds);
                for (int ii = 0; ii < articles.length; ii++) {
                    Article article = articles[ii];
                    ArrayIntSet memberIds = (article.getArticleName().indexOf("female") == -1) ?
                        maleIds : femaleIds;
                    ArrayIntSet ownerIds = BangServer.itemrepo.getItemOwners(memberIds, article);
                    int count = memberIds.size() - ownerIds.size();
                    _cost[0] += (catarts[ii].scrip * count);
                    _cost[1] += (catarts[ii].coins * count);
                    if (buy && count > 0) {
                        _receiverIds[ii] = (ArrayIntSet)memberIds.clone();
                        _receiverIds[ii].removeAll(ownerIds);
                    }
                }
            }
            public void handleSuccess () {
                // update the gang's configured outfit
                _gangobj.setOutfit(outfit);

                if (!buy) {
                    // if we're not buying, just report the price quote
                    listener.requestProcessed(_cost);
                } else if (_cost[0] == 0 && _cost[1] == 0) {
                    listener.requestProcessed(new int[] { 0, 0 });
                } else {
                    try {
                        buyOutfits(handle, admin, _cost[0], _cost[1], _receiverIds,
                            articles, listener);
                    } catch (InvocationException e) {
                        listener.requestFailed(e.getMessage());
                    }
                }
            }
            protected int[] _cost = new int[2];
            protected ArrayIntSet[] _receiverIds = new ArrayIntSet[articles.length];
        });
    }

    /**
     * Having determined which gang members need parts of the outfit and computed the cost,
     * uses fund from the gang's coffers to buy the outfits.
     */
    protected void buyOutfits (
        final Handle handle, final boolean admin, final int scripCost, final int coinCost,
        final ArrayIntSet[] userIds, final Article[] articles,
        final InvocationService.ResultListener listener)
        throws InvocationException
    {
        new GangFinancialAction(_gangobj, admin, scripCost, coinCost) {
            protected int getCoinType () {
                return CoinTransaction.GANG_OUTFIT_PURCHASE;
            }
            protected String getCoinDescrip () {
                return "m.gang_outfits";
            }

            protected String persistentAction ()
                throws PersistenceException {
                ArrayIntSet memberIds = new ArrayIntSet();
                for (int ii = 0; ii < userIds.length; ii++) {
                    if (userIds[ii] == null) {
                        continue;
                    }
                    memberIds.addAll(userIds[ii]);
                    BangServer.itemrepo.insertItems(articles[ii], userIds[ii], _items);
                }
                _memberCount = memberIds.size();
                _entryId = BangServer.gangrepo.insertHistoryEntry(_gangId,
                        MessageBundle.compose(
                            "m.outfit_entry",
                            MessageBundle.taint(handle),
                            getMoneyDesc(scripCost, coinCost),
                            MessageBundle.tcompose("m.member_count", _memberCount),
                            MessageBundle.tcompose("m.articles", _items.size())));
                return null;
            }
            protected void rollbackPersistentAction ()
                throws PersistenceException {
                if (!_items.isEmpty()) {
                    ArrayIntSet itemIds = new ArrayIntSet();
                    for (Item item : _items) {
                        itemIds.add(item.getItemId());
                    }
                    BangServer.itemrepo.deleteItems(itemIds, "rollback");
                }
                if (_entryId > 0) {
                    BangServer.gangrepo.deleteHistoryEntry(_entryId);
                }
            }

            protected void actionCompleted () {
                String source = MessageBundle.qualify(GANG_MSGS,
                    MessageBundle.tcompose("m.gang_title", _gangobj.name));
                for (Item item : _items) {
                    BangServer.playmgr.deliverItem(item, source);
                }
                listener.requestProcessed(new int[] { _memberCount, _items.size() });
            }
            protected void actionFailed (String cause) {
                listener.requestFailed(cause);
            }

            protected ArrayList<Item> _items = new ArrayList<Item>();
            protected int _memberCount;
            protected int _entryId;
        }.start();
    }

    /**
     * Makes sure the supplied client is either <code>null</code> (for locally generated requests)
     * or a peer, rather than a user.  Logs a warning and throws an exception if not.
     */
    protected void verifyLocalOrPeer (ClientObject caller)
        throws InvocationException
    {
        if (caller instanceof PlayerObject) {
            log.warning("Player tried to access gang peer service [who=" + caller.who() + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
    }

    /**
     * Makes sure the specified user can change the rank of or remove the specified other member,
     * logging a warning and throwing an exception if not.
     *
     * @return the target user's gang member entry.
     */
    protected GangMemberEntry verifyCanChange (Handle handle, Handle target)
        throws InvocationException
    {
        GangMemberEntry changee = verifyInGang(target);
        if (handle == null) { // special case for auto-promotions, self-removals
            return changee;
        }
        GangMemberEntry changer = verifyIsLeader(handle);
        if (!changee.canChangeStatus(changer)) {
            log.warning("User cannot change member [changer=" + changer +
                ", changee=" + changee + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        return changee;
    }

    /**
     * Makes sure the specified user is a leader of this gang, logging a warning and throwing an
     * exception if not.
     *
     * @return the user's gang member entry.
     */
    protected GangMemberEntry verifyIsLeader (Handle handle)
        throws InvocationException
    {
        GangMemberEntry entry = verifyInGang(handle);
        if (entry.rank != LEADER_RANK) {
            log.warning("User not leader [entry=" + entry + ", gang=" + this + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        return entry;
    }

    /**
     * Makes sure the specified user is in this gang, logging a warning and throwing an exception
     * if not.
     *
     * @return the user's gang member entry.
     */
    protected GangMemberEntry verifyInGang (Handle handle)
        throws InvocationException
    {
        GangMemberEntry entry = _gangobj.members.get(handle);
        if (entry == null) {
            log.warning("User not in gang [handle=" + handle + ", gang=" + this + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        return entry;
    }

    /**
     * Loads the gang data from the database, since we hold the gang lock.
     */
    protected void loadFromDatabase ()
    {
        BangServer.invoker.postUnit(
            new RepositoryUnit("loadGang") {
                public void invokePersist () throws PersistenceException {
                    _grec = BangServer.gangrepo.loadGang(_gangId, true);
                }
                public void handleSuccess () {
                    createGangObject(_grec);
                }
                public void handleFailure (Exception cause) {
                    initFailed(cause);
                }
                protected GangRecord _grec;
            });
    }

    /**
     * Creates and registers the gang object once its data has been retrieved from the database.
     */
    protected void createGangObject (GangRecord record)
    {
        // create and populate the object
        _gangobj = new GangObject();
        _gangobj.gangId = _gangId;
        _gangobj.name = record.getName();
        _gangobj.founded = record.founded.getTime();
        _gangobj.statement = record.statement;
        _gangobj.url = record.url;
        _gangobj.avatar = record.avatar;
        _gangobj.scrip = record.scrip;
        _gangobj.coins = record.coins;
        _gangobj.notoriety = record.notoriety;
        _gangobj.notorietyRank = getNotorietyRank(record.notoriety);
        _gangobj.outfit = record.outfit;
        _gangobj.members = new DSet<GangMemberEntry>(record.members.iterator());

        // find out which members are online (and listen for updates)
        for (GangMemberEntry entry : _gangobj.members) {
            initTownIndex(entry);
        }
        BangServer.addPlayerObserver(this);
        BangServer.peermgr.addPlayerObserver(this);

        // register the service for peers
        _gangobj.gangPeerService =
            (GangPeerMarshaller)BangServer.invmgr.registerDispatcher(
                new GangPeerDispatcher(this));

        // register and announce
        BangServer.omgr.registerObject(_gangobj);
        log.info("Initialized gang object " + this + ".");

        // start the interval to calculate top-ranked members
        _rankval = new Interval(BangServer.omgr) {
            public void expired () {
                SaloonManager.refreshTopRanked(_gangobj,
                    "GANG_MEMBERS", "RATINGS.PLAYER_ID = GANG_MEMBERS.PLAYER_ID and " +
                    "GANG_MEMBERS.GANG_ID = " + _gangId, TOP_RANKED_LIST_SIZE);
                _gangobj.setNotorietyRank(getNotorietyRank(_gangobj.notoriety));
            }
        };
        _rankval.schedule(1000L, RANK_REFRESH_INTERVAL);

        // create the interval to unload the gang
        _unloadval = new Interval(BangServer.omgr) {
            public void expired () {
                unload();
            }
        };
        maybeScheduleUnload();

        // notify the listeners
        didInit();
    }

    /**
     * Called when a player logs in or out of a town server.
     */
    protected void playerLocationChanged (Name name, int townIndex)
    {
        GangMemberEntry entry = (name == null) ? null : _gangobj.members.get(name);
        if (entry == null) {
            return;
        }
        entry.townIdx = (byte)townIndex;
        _gangobj.updateMembers(entry);
        if (townIndex == -1) {
            maybeScheduleUnload();
        } else {
            maybeCancelUnload();
        }
    }

    /**
     * Starts the unload interval if there are no members online.
     */
    protected void maybeScheduleUnload ()
    {
        for (GangMemberEntry entry : _gangobj.members) {
            if (entry.townIdx != -1) {
                return;
            }
        }
        _unloadval.schedule(UNLOAD_INTERVAL);
    }

    /**
     * Cancels the unload interval and reacquires the lock if we're in the process of releasing it.
     */
    protected void maybeCancelUnload ()
    {
        _unloadval.cancel();
        if (_releasing != null) {
            BangServer.peermgr.reacquireLock(_releasing);
        }
    }

    /**
     * Starts the process of unloading.
     */
    protected void unload ()
    {
        // attempt to drop the lock
        BangServer.peermgr.releaseLock(_releasing = new Lock("gang", _gangId),
            new ResultListener<String>() {
                public void requestCompleted (String result) {
                    _releasing = null;
                    if (result == null) {
                        shutdown();
                    } else {
                        maybeScheduleUnload();
                    }
                }
                public void requestFailed (Exception cause) {
                    _releasing = null;
                    log.warning("Failed to release lock [handler=" + this + ", error=" +
                        cause + "].");
                    maybeScheduleUnload();
                }
            });
    }

    /**
     * Subscribe to another node's gang object, since they hold the lock.
     */
    protected void subscribeToPeer (String nodeName)
    {
        _nodeName = nodeName;
        _client = BangServer.peermgr.getPeerClient(nodeName);
        if (_client == null) {
            log.warning("Not connected to peer that holds gang lock?! [node=" +
                nodeName + ", gangId=" + _gangId + "].");
            initFailed(new InvocationException(INTERNAL_ERROR));
            return;
        }
        BangServer.peermgr.subscribeToGang(nodeName, _gangId,
            new ResultListener<GangObject>() {
                public void requestCompleted (GangObject result) {
                    _gangobj = result;
                    _proxy = PeerUtil.createProviderProxy(
                        GangPeerProvider.class, _gangobj.gangPeerService, _client);

                    log.info("Subscribed to gang " + this + " on " + _nodeName + ".");
                    didInit();
                }
                public void requestFailed (Exception cause) {
                    initFailed(cause);
                }
            });
    }

    /**
     * Notifies the listeners with the newly acquired gang object.
     */
    protected void didInit ()
    {
        _gangobj.addListener(this);
        _listeners.requestCompleted(_gangobj);
        _listeners = null;
    }

    /**
     * Notifies the listeners of failure and unmaps the handler.
     */
    protected void initFailed (Exception cause)
    {
        log.warning("Failed to initialize gang [handler=" + this + ", error=" +
            cause + "].");
        _listeners.requestFailed(cause);
        BangServer.gangmgr.unmapGang(_gangId);
    }

    /**
     * Unsubscribes from the peer and unmaps the gang.
     */
    protected void unsubscribeFromPeer ()
    {
        _gangobj.removeListener(this);
        BangServer.peermgr.unproxyRemoteObject(_nodeName, _gangobj.remoteOid);

        log.info("Unsubscribed from gang " + this + ".");
        BangServer.gangmgr.unmapGang(_gangId);
    }

    /**
     * Shuts the gang down and unmaps it.
     */
    protected void shutdown ()
    {
        _rankval.cancel();
        _unloadval.cancel();

        BangServer.removePlayerObserver(this);
        BangServer.peermgr.removePlayerObserver(this);

        BangServer.invmgr.clearDispatcher(_gangobj.gangPeerService);
        _gangobj.destroy();

        log.info("Gang shutdown " + this + ".");
        BangServer.gangmgr.unmapGang(_gangId);
    }

    /**
     * Updates the object used to track the leader's avatar.
     *
     * @param player if non-null, a candidate player object in the process of resolution.
     */
    protected void updateAvatarUpdater (PlayerObject player)
    {
        GangMemberEntry leader = _gangobj.getSeniorLeader();
        PlayerObject user = (leader == null) ? null :
            ((player != null && player.playerId == leader.playerId) ?
                player : BangServer.lookupPlayer(leader.playerId));
        if (user != null) {
            if (_avupdater == null || _avupdater.getPlayerId() != user.playerId) {
                if (_avupdater != null) {
                    _avupdater.remove();
                }
                (_avupdater = new AvatarUpdater(this)).add(user);
            }
        } else if (_avupdater != null) {
            _avupdater.remove();
            _avupdater = null;
        }
    }

    /**
     * Handles a successful response to an invitation.
     */
    protected void handleInviteSuccess (
        Handle handle, int playerId, Handle inviter, GangMemberRecord mrec,
        InvocationService.ConfirmListener listener)
    {
        // TODO: notify the inviter if he's on another server
        PlayerObject invobj = BangServer.lookupPlayer(inviter);
        if (invobj != null) {
            SpeakProvider.sendInfo(invobj, GANG_MSGS,
                MessageBundle.tcompose(
                    (mrec == null) ? "m.member_rejected" : "m.member_accepted",
                    handle, _gangobj.name));
        }
        if (mrec != null) {
            GangMemberEntry entry =
                new GangMemberEntry(handle, playerId, MEMBER_RANK, mrec.joined, 0);
            initTownIndex(entry);
            _gangobj.addToMembers(entry);
            if (entry.townIdx == -1) {
                maybeScheduleUnload();
            } else {
                maybeCancelUnload();
            }
        }
        listener.requestProcessed();
    }

    /**
     * Sets the town index for the supplied member entry based on where the member is
     * logged in (if anywhere).
     */
    protected void initTownIndex (GangMemberEntry entry)
    {
        PlayerObject user = BangServer.lookupPlayer(entry.handle);
        if (user != null) {
            entry.townIdx = (byte)ServerConfig.townIndex;
            return;
        }
        Tuple<BangClientInfo, Integer> result =
            BangServer.peermgr.locateRemotePlayer(entry.handle);
        entry.townIdx = (result == null) ? -1 : result.right.byteValue();
    }

    /**
     * Continues the process of removing a member from a gang, perhaps after having promoted a
     * player to avoid leaving a gang leaderless.
     */
    protected void continueRemovingFromGang (
        final Handle handle, final GangMemberEntry entry, final GangMemberEntry senior,
        final boolean delete, final InvocationService.ConfirmListener listener)
    {
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                BangServer.gangrepo.deleteMember(entry.playerId);
                if (delete) {
                    BangServer.gangrepo.deleteGang(_gangId);
                } else {
                    if (senior != null) {
                        _avatar = BangServer.lookrepo.loadSnapshot(senior.playerId);
                    }
                    BangServer.gangrepo.insertHistoryEntry(_gangId, (handle == null) ?
                        MessageBundle.tcompose("m.left_entry", entry.handle) :
                        MessageBundle.tcompose("m.expelled_entry", handle, entry.handle));
                }
            }

            public void handleSuccess () {
                _gangobj.startTransaction();
                try {
                    if (_avatar != null) {
                        _gangobj.setAvatar(_avatar);
                    }
                    _gangobj.removeFromMembers(entry.handle);
                } finally {
                    _gangobj.commitTransaction();
                }
                // if deleting, remove from Hideout directory and shut down immediately
                if (delete) {
                    BangServer.hideoutmgr.removeGang(_gangobj.name);
                    shutdown();
                } else {
                    maybeScheduleUnload();
                }
                listener.requestProcessed();
            }
            public String getFailureMessage () {
                return "Failed to remove member from gang [gang=" + this + ", handle=" +
                    handle + ", entry=" + entry + ", delete=" + delete + "].";
            }
            protected AvatarInfo _avatar;
        });
    }

    /**
     * Returns the rank corresponding to the supplied notoriety level.
     */
    protected static byte getNotorietyRank (int notoriety)
    {
        return (byte)BangServer.ratingmgr.getRank(NOTORIETY_IDENT, notoriety);
    }

    /**
     * Returns a translatable string describing the identified amounts (at least one of which must
     * be nonzero).
     */
    protected static String getMoneyDesc (int scrip, int coins)
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

    /** The id of our gang. */
    protected int _gangId;

    /** Listeners for the gang object. */
    protected ResultListenerList<GangObject> _listeners = new ResultListenerList<GangObject>();

    /** The name of the node controlling the gang object. */
    protected String _nodeName;

    /** The peer client, if our gang object is a proxy for a remote one. */
    protected Client _client;

    /** The proxy object implementing {@link GangPeerProvider} to forward requests to the peer. */
    protected GangPeerProvider _proxy;

    /** The gang object, when resolved. */
    protected GangObject _gangobj;

    /** The interval that refreshes the list of top-ranked members. */
    protected Interval _rankval;

    /** The interval that starts the unloading process. */
    protected Interval _unloadval;

    /** If non-null, we're trying to release this lock. */
    protected Lock _releasing;

    /** Keeps the gang's senior leader avatar up-to-date. */
    protected AvatarUpdater _avupdater;

    /** The frequency with which we update the top-ranked member lists. */
    protected static final long RANK_REFRESH_INTERVAL = 60 * 60 * 1000L;

    /** The size of the top-ranked member lists. */
    protected static final int TOP_RANKED_LIST_SIZE = 10;

    /** In order to prevent rapid loading and unloading, we wait this long after the last gang
     * member has logged off of the cluster to unload the gang. */
    protected static final long UNLOAD_INTERVAL = 30 * 60 * 1000L;
}
