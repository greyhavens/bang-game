//
// $Id$

package com.threerings.bang.gang.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.RepositoryUnit;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.ResultListenerList;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.peer.data.NodeObject.Lock;
import com.threerings.presents.peer.server.PeerManager.DroppedLockObserver;
import com.threerings.presents.peer.util.PeerUtil;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.util.PersistingUnit;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.server.SpeakDispatcher;
import com.threerings.crowd.chat.server.SpeakHandler;
import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.crowd.chat.server.SpeakUtil;

import com.threerings.coin.server.CoinExOffer;
import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BangClientInfo;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.BuckleUpgrade;
import com.threerings.bang.data.EntryReplacedEvent;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Notification;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.WeightClassUpgrade;
import com.threerings.bang.server.BangPeerManager.RemotePlayerObserver;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.BangServer.PlayerObserver;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.PeerFinancialAction;
import com.threerings.bang.server.persist.ProxyFinancialAction;

import com.threerings.bang.admin.data.ServerConfigObject;
import com.threerings.bang.admin.server.RuntimeConfig;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.game.data.scenario.ScenarioInfo;

import com.threerings.bang.saloon.server.SaloonManager;
import com.threerings.bang.saloon.server.TableGameManager;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.GangInvite;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.GangPeerMarshaller;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HistoryEntry;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.gang.data.RentalGood;
import com.threerings.bang.gang.data.WeightClassUpgradeGood;
import com.threerings.bang.gang.server.persist.GangFinancialAction;
import com.threerings.bang.gang.server.persist.GangMemberRecord;
import com.threerings.bang.gang.server.persist.GangRecord;
import com.threerings.bang.gang.util.GangUtil;

import static com.threerings.bang.Log.*;

/**
 * Manages a single gang from resolution to destruction.
 */
public class GangHandler
    implements DroppedLockObserver, PlayerObserver, RemotePlayerObserver, MessageListener,
        AttributeChangeListener, SetListener, ObjectDeathListener, SpeakHandler.SpeakerValidator,
        GangPeerProvider, GangCodes
{
    /** The amount of time in hours required between leader commands for each leader level. */
    public static final int[] LEADER_LEVEL_WAITS = {
        0, 24, 24, 12, 12, 6, 6, 3, 3, 1, 0
    };

    /**
     * Convenience function for persisting an increment of a gang member's leader level.  Must be
     * called on the invoker thread.
     */
    public static void incLeaderLevel (GangObject gangobj, Handle handle)
        throws PersistenceException
    {
        if (handle != null) {
            GangMemberEntry leader = gangobj.members.get(handle);
            if (leader != null && leader.leaderLevel < LEADER_LEVEL_WAITS.length - 1) {
                // if they've waited twice as long as necessary, they get a double bump in level
                long doubleTime = leader.lastLeaderCommand +
                    2 * LEADER_LEVEL_WAITS[leader.leaderLevel] * ONE_HOUR;
                if (leader.leaderLevel > 0 && doubleTime < System.currentTimeMillis()) {
                    leader.leaderLevel =
                        Math.min(LEADER_LEVEL_WAITS.length-1, leader.leaderLevel+2);
                } else {
                    leader.leaderLevel++;
                }
                leader.lastLeaderCommand = System.currentTimeMillis();
                BangServer.gangrepo.updateLeaderLevel(leader.playerId, leader.leaderLevel);
            }
        }
    }

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
    public GangHandler (
        final GangRecord grec, final PlayerObject creator,
        final InvocationService.ConfirmListener listener)
    {
        _gangId = grec.gangId;

        // add a listener to initialize the player and report success
        getGangObject(new ResultListener<GangObject>() {
            public void requestCompleted (GangObject result) {
                listener.requestProcessed();
            }
            public void requestFailed (Exception cause) {
                listener.requestFailed(INTERNAL_ERROR);
            }
        });

        // initialize immediately if not running in peer mode
        if (BangServer.peermgr == null) {
            createGangObject(grec);
            return;
        }

        // otherwise, acquire the lock before continuing
        BangServer.peermgr.acquireLock(new Lock("gang", _gangId),
            new ResultListener<String>() {
                public void requestCompleted (String result) {
                    if (result.equals(ServerConfig.nodename)) {
                        createGangObject(grec);
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
    public void getHistoryEntries (final int offset, final int count, final String filter,
            final InvocationService.ResultListener listener)
    {
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                _entries = BangServer.gangrepo.loadHistoryEntries(_gangId, offset, count, filter);
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

    /**
     * Posts an immediate buy offer on the gold exchange for the town the user is situated in.
     * This must be coordinated between the peer on which the used is logged in and the
     * controlling peer.
     */
    public void postOffer (PlayerObject user, int coins, int pricePerCoin,
            InvocationService.ResultListener listener)
        throws InvocationException
    {
        verifyIsLeader(user.handle);

        CoinExOffer offer = new CoinExOffer();
        offer.accountName = _gangobj.getCoinAccount();
        offer.gameName = user.handle.toString();
        offer.buy = true;
        offer.volume = (short)Math.min(coins, Short.MAX_VALUE);
        offer.price = (short)Math.min(pricePerCoin, Short.MAX_VALUE);
        BangServer.coinexmgr.postOffer(_gangobj, offer, true, new ResultAdapter<Object>(listener));
    }

    /**
     * Requests to leave the gang.  The peer on which the user is logged in needs to know when
     * users are leaving voluntarily, as opposed to being expelled.
     */
    public void leaveGang (
        final PlayerObject user, final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        _leaverIds.add(user.playerId);
        getPeerProvider().removeFromGang(null, null, user.handle,
            new InvocationService.ConfirmListener() {
                public void requestProcessed () {
                    // we will remove the player's id from leaverIds when we receive notification
                    // that the member entry has been removed
                    listener.requestProcessed();
                }
                public void requestFailed (String cause) {
                    _leaverIds.remove(user.playerId);
                    listener.requestFailed(cause);
                }
            });
    }

    @Override // documentation inherited
    public String toString ()
    {
        return (_gangobj == null ? "" : (_gangobj.name + " ")) + "(" + _gangId + ")";
    }

    // documentation inherited from interface DroppedLockObserver
    public void droppedLock (Lock lock)
    {
        // if we lose the lock, we have to shut down immediately and re-resolve
        if (lock.type.equals("gang") && ((Integer)lock.id) == _gangId) {
            shutdown();
            BangServer.gangmgr.resolveGang(_gangId);
        }
    }

    // documentation inherited from interface PlayerObserver
    public void playerLoggedOn (PlayerObject user)
    {
        playerLocationChanged(user.handle, -1, ServerConfig.townIndex);
    }

    // documentation inherited from interface PlayerObserver
    public void playerLoggedOff (PlayerObject user)
    {
        playerLocationChanged(user.handle, ServerConfig.townIndex, -1);
    }

    // documentation inherited from interface PlayerObserver
    public void playerChangedHandle (PlayerObject user, Handle oldHandle)
    {
        // we handle this the same way for local and remote users
        remotePlayerChangedHandle(ServerConfig.townIndex, oldHandle, user.handle);
    }

    // documentation inherited from interface RemotePlayerObserver
    public void remotePlayerLoggedOn (int townIndex, BangClientInfo info)
    {
        playerLocationChanged(info.visibleName, -1, townIndex);
    }

    // documentation inherited from interface RemotePlayerObserver
    public void remotePlayerLoggedOff (int townIndex, BangClientInfo info)
    {
        playerLocationChanged(info.visibleName, townIndex, -1);
    }

    // documentation inherited from interface RemotePlayerObserver
    public void remotePlayerChangedHandle (int townIndex, Handle oldHandle, Handle newHandle)
    {
        GangMemberEntry oldEntry = _gangobj.members.get(oldHandle);
        if (oldEntry != null) {
            GangMemberEntry newEntry = (GangMemberEntry)oldEntry.clone();
            newEntry.handle = newHandle;
            BangServer.omgr.postEvent(new EntryReplacedEvent<GangMemberEntry>(
                _gangobj, GangObject.MEMBERS, oldHandle, newEntry));
        }
    }

    // documentation inherited from interface MessageListener
    public void messageReceived (MessageEvent event)
    {
        if (!event.getName().equals(ChatCodes.CHAT_NOTIFICATION)) {
            return;
        }
        ChatMessage msg = (ChatMessage)event.getArgs()[0];
        if (!(msg instanceof UserMessage) || ((UserMessage)msg).mode != ChatCodes.BROADCAST_MODE) {
            return;
        }
        // we need to forward the broadcast to all members on this server who aren't in the Hideout
        UserMessage umsg = (UserMessage)msg;
        for (GangMemberEntry member : _gangobj.members) {
            if (member.townIdx == ServerConfig.townIndex && !member.isInHideout()) {
                PlayerObject user = BangServer.lookupPlayer(member.playerId);
                if (user != null) {
                    BangServer.chatprov.deliverTell(user, umsg);
                } else {
                    log.warning("Member mistakenly marked as online [member=" + member + "].");
                }
            }
        }
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(GangObject.STATEMENT) || name.equals(GangObject.URL) ||
            name.equals(GangObject.AVATAR) || name.equals(GangObject.NOTORIETY) ||
            name.equals(GangObject.BUCKLE)) {
            // invalidate any cached gang info
            gangInfoChanged();
        }
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        String name = event.getName();
        if (name.equals(GangObject.INVENTORY)) {
            Item item = (Item)event.getEntry();
            if (item instanceof WeightClassUpgrade && _client == null) {
                // update weight class and notoriety on purchase of upgrade
                _gangobj.setNotoriety(GangUtil.getNotorietyLevel(
                    _gangobj.getWeightClass(), _notoriety));
                _gangobj.setRentMultiplier(RuntimeConfig.server.rentMultiplier[
                        _gangobj.getWeightClass()]);
                _gangobj.setArticleRentMultiplier(RuntimeConfig.server.articleRentMultiplier[
                        _gangobj.getWeightClass()]);
                gangInfoChanged();
            } else if (!(item instanceof WeightClassUpgrade) && !(item instanceof BuckleUpgrade) &&
                    !(item instanceof BucklePart)) {
                distributeRental(item);
            }
            return;

        } else if (!name.equals(GangObject.MEMBERS)) {
            return;
        }
        // invalidate any cached gang info
        gangInfoChanged();
        if (event instanceof EntryReplacedEvent.ReplacementAddedEvent) {
            return; // no need to reset gang fields for handle change
        }

        // set the user's gang fields if he's online
        GangMemberEntry entry = (GangMemberEntry)event.getEntry();
        PlayerObject user = BangServer.lookupPlayer(entry.playerId);
        if (user != null) {
            initPlayer(user, entry);
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
        // remove all outstanding gang invitations
        ArrayList<Comparable> ikeys = new ArrayList<Comparable>();
        for (Notification note : user.notifications) {
            if (note instanceof GangInvite) {
                ikeys.add(note.getKey());
            }
        }

        // set the gang fields of their player object
        user.startTransaction();
        try {
            for (Comparable ikey : ikeys) {
                user.removeFromNotifications(ikey);
            }
            user.setGangId(_gangId);
            user.setGangRank(entry.rank);
            user.setGangTitle(entry.title);
            user.setGangCommandOrder(entry.commandOrder);
            user.setGangOid(_gangobj.getOid());
        } finally {
            user.commitTransaction();
        }

        // if they're in a place, update their occupant info
        PlaceManager plmgr = BangServer.plreg.getPlaceManager(user.getPlaceOid());
        if (plmgr != null) {
            BangOccupantInfo boi = (BangOccupantInfo)plmgr.getOccupantInfo(user.getOid());
            boi.gangId = _gangId;
            plmgr.updateOccupantInfo(boi);

            // if they're in the Hideout, update their avatar
            if (plmgr instanceof HideoutManager) {
                getPeerProvider().memberEnteredHideout(null, user.handle, boi.avatar);
            }
        }

        // if they're the senior leader, start tracking their avatar
        updateAvatar(user);
    }

    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (!event.getName().equals(GangObject.MEMBERS) || event instanceof EntryReplacedEvent) {
            return; // no need to react to handle changes
        }
        // invalidate any cached gang info
        gangInfoChanged();

        // consider auto-promoting
        maybeAutoPromote();

        // make sure we're tracking the right avatar
        updateAvatar(null);

        // determine if the member was leaving voluntarily
        GangMemberEntry oentry = (GangMemberEntry)event.getOldEntry();
        boolean leaving = _leaverIds.remove(oentry.playerId);

        // clear the user's gang fields, reimburse part of his donation (if he's getting kicked
        // out), and purge his looks if he's online
        Handle handle = (Handle)event.getKey();
        PlayerObject user = BangServer.lookupPlayer(handle);
        if (user == null) {
            return;
        }
        int[] refund = oentry.getDonationReimbursement();
        if (leaving) {
            refund[0] = refund[1] = 0;
        }
        ArrayIntSet removals = new ArrayIntSet();
        ArrayList<Look> modified = stripLooks(user.inventory, user.looks, removals);
        user.startTransaction();
        try {
            user.setGangId(0);
            user.setGangOid(0);
            if (refund[0] > 0) {
                user.setScrip(user.scrip + refund[0]);
            }
            if (refund[1] > 0) {
                user.setCoins(user.coins + refund[1]);
            }
            for (Look look : modified) {
                user.updateLooks(look);
            }
            for (Item item : user.inventory.toArray(new Item[user.inventory.size()])) {
                if (removals.contains(item.getItemId())) {
                    user.removeFromInventory(item.getKey());
                }
            }
        } finally {
            user.commitTransaction();
        }

        // remove them from any pending table games
        if (_tmgr != null) {
            _tmgr.clearPlayer(user.getOid());
        }

        // if they're in a place, update their occupant info
        PlaceManager plmgr = BangServer.plreg.getPlaceManager(user.getPlaceOid());
        if (plmgr != null) {
            BangOccupantInfo boi = (BangOccupantInfo)plmgr.getOccupantInfo(user.getOid());
            boi.gangId = 0;
            plmgr.updateOccupantInfo(boi);
        }
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (!event.getName().equals(GangObject.MEMBERS)) {
            return;
        }
        // invalidate any cached gang info
        gangInfoChanged();

        // consider auto-promoting
        maybeAutoPromote();

        // make sure we're tracking the right avatar
        updateAvatar(null);

        // update the user's gang fields if he's online
        GangMemberEntry entry = (GangMemberEntry)event.getEntry();
        PlayerObject user = BangServer.lookupPlayer(entry.playerId);
        if (user == null) {
            return;
        }
        if (user.gangRank != entry.rank || user.gangTitle != entry.title) {
            user.startTransaction();
            try {
                user.setGangRank(entry.rank);
                user.setGangCommandOrder(entry.commandOrder);
                user.setGangTitle(entry.title);
            } finally {
                user.commitTransaction();
            }
        }
        deliverGangItems(user);
    }

    // documentation inherited from interface ObjectDeathListener
    public void objectDestroyed (ObjectDestroyedEvent event)
    {
        if (_client != null) {
            unsubscribeFromPeer();
        }
    }

    // from interface SpeakHandler.SpeakerValidator
    public boolean isValidSpeaker (DObject speakobj, ClientObject speaker, byte mode)
    {
        return _gangobj.members.containsKey(((PlayerObject)speaker).handle);
    }

    // documentation inherited from interface GangPeerProvider
    public void grantAces (ClientObject caller, final Handle handle, final int aces)
    {
        // make sure it comes from this server or a peer
        GangMemberEntry member = null;
        try {
            verifyLocalOrPeer(caller);
            member = verifyInGang(handle);
        } catch (InvocationException e) {
            return;
        }

        // refresh the gang's last played time on all servers
        BangServer.hideoutmgr.activateGang(_gangobj.name);

        // update the database
        final GangMemberEntry entry = member;
        BangServer.invoker.postUnit(new RepositoryUnit() {
            public void invokePersist ()
                throws PersistenceException {
                // notoriety points are simply accumulated aces
                BangServer.gangrepo.grantAces(_gangId, aces);
                BangServer.gangrepo.addNotoriety(_gangId, entry.playerId, aces);
            }
            public void handleSuccess () {
                GangMemberEntry member = _gangobj.members.get(handle);
                _gangobj.startTransaction();
                try {
                    _gangobj.setAces(_gangobj.aces + aces);
                    _gangobj.setNotoriety(GangUtil.getNotorietyLevel(
                        _gangobj.getWeightClass(), (_notoriety += aces)));
                    if (member != null) {
                        member.notoriety += aces;
                        _gangobj.updateMembers(member);
                    }
                } finally {
                    _gangobj.commitTransaction();
                }
            }
            public void handleFailure (Exception cause) {
                log.warning("Failed to grant aces [gang=" + GangHandler.this + ", handle=" +
                    handle + ", aces=" + aces + ", error=" + cause + "].");
            }
        });
    }

    /**
     * Sets the gang notoriety, should only be called by {@link Gang Manager}.
     */
    public void setNotoriety (int notoriety)
    {
        _notoriety = notoriety;
        _gangobj.setNotoriety(GangUtil.getNotorietyLevel(_gangobj.getWeightClass(), _notoriety));
    }

    // documentation inherited from interface GangPeerProvider
    public void setAvatar (ClientObject caller, int playerId, AvatarInfo info)
    {
        // make sure it comes from this server or a peer
        try {
            verifyLocalOrPeer(caller);
        } catch (InvocationException e) {
            return;
        }

        // if it matches the senior leader, set it immediately
        GangMemberEntry leader = _gangobj.getSeniorLeader();
        if (leader != null && leader.playerId == playerId) {
            _gangobj.setAvatar(info);
            _avatarId = playerId;
        }
    }

    // documentation inherited from interface GangPeerProvider
    public void memberEnteredHideout (ClientObject caller, Handle handle, AvatarInfo info)
    {
        // make sure it comes from this server or a peer
        GangMemberEntry member = null;
        try {
            verifyLocalOrPeer(caller);
            member = verifyInGang(handle);
        } catch (InvocationException e) {
            return;
        }
        member.avatar = info;
        _gangobj.startTransaction();
        try {
            _gangobj.updateMembers(member);

            // removed any expired items from the inventory
            ArrayList<Comparable> removals = null;
            long now = System.currentTimeMillis();
            for (Item item : _gangobj.inventory) {
                if (item.isExpired(now)) {
                    if (removals == null) {
                        removals = new ArrayList<Comparable>();
                    }
                    removals.add(item.getKey());
                }
            }
            if (removals != null) {
                for (Comparable key : removals) {
                    _gangobj.removeFromInventory(key);
                }
            }
        } finally {
            _gangobj.commitTransaction();
        }

    }

    public void bodyLeft (int bodyOid)
    {
        if (_tmgr != null) {
            _tmgr.clearPlayer(bodyOid);
        }
    }

    // documentation inherited from interface GangPeerProvider
    public void memberLeftHideout (ClientObject caller, Handle handle)
    {
        // make sure it comes from this server or a peer
        GangMemberEntry member = null;
        try {
            verifyLocalOrPeer(caller);
            member = verifyInGang(handle);
        } catch (InvocationException e) {
            return;
        }
        member.avatar = null;
        _gangobj.updateMembers(member);
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
        int maxMembers = WEIGHT_CLASSES[_gangobj.getWeightClass()].maxMembers;
        if (_gangobj.members.containsKey(target)) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.already_member_this", target));
        } else if (_gangobj.members.size() >= maxMembers) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.too_many_members", String.valueOf(maxMembers)));
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
        final int maxMembers = WEIGHT_CLASSES[_gangobj.getWeightClass()].maxMembers;
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                _error = BangServer.gangrepo.deleteInvite(_gangId, maxMembers, playerId, accept);
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
                } else if (_deleting && _mrec != null) {
                    // the gang has been deleted already
                    listener.requestFailed("e.invite_removed");
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
    public void sendSpeak (ClientObject caller, Handle handle, String message, byte mode)
    {
        // make sure it comes from this server or a peer and that they're in the gang
        try {
            verifyLocalOrPeer(caller);
            verifyInGang(handle);
        } catch (InvocationException e) {
            return;
        }

        // speak!
        SpeakUtil.sendSpeak(_gangobj, handle, null, message, mode);
    }

    // documentation inherited from interface GangPeerProvider
    public void setStatement (
        ClientObject caller, final Handle handle, final String statement,
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
                incLeaderLevel(_gangobj, handle);
            }
            public void handleSuccess () {
                GangMemberEntry leader = _gangobj.members.get(handle);
                _gangobj.startTransaction();
                try {
                    _gangobj.setStatement(statement);
                    _gangobj.setUrl(url);
                    _gangobj.updateMembers(leader);
                } finally {
                    _gangobj.commitTransaction();
                }
                listener.requestProcessed();
            }
        });
    }

    // documentation inherited from interface GangPeerProvider
    public void setBuckle (
        ClientObject caller, final Handle handle, final BucklePart[] parts,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure it comes from a leader
        verifyIsLeader(handle);

        // form the fingerprint before fiddling with the array
        final BuckleInfo buckle = GangUtil.getBuckleInfo(parts);

        // verify and count all of the parts, clear out the ones that haven't changed
        final int[] partIds = new int[parts.length];
        int[] ccounts = new int[AvatarLogic.BUCKLE_PARTS.length];
        boolean changed = false;
        for (int ii = 0; ii < parts.length; ii++) {
            BucklePart opart = parts[ii];
            int itemId = opart.getItemId();
            Item item = _gangobj.inventory.get(itemId);
            if (!opart.isEquivalent(item)) {
                log.warning("Invalid part for buckle [gang=" + this + ", handle=" + handle +
                    ", opart=" + opart + ", npart=" + item + "].");
                throw new InvocationException(INTERNAL_ERROR);
            } else if (IntListUtil.contains(partIds, itemId)) {
                log.warning("Duplicate part in buckle [gang=" + this + ", handle=" + handle +
                    ", parts=" + StringUtil.toString(parts) + "].");
                throw new InvocationException(INTERNAL_ERROR);
            }
            partIds[ii] = itemId;
            ccounts[AvatarLogic.getPartIndex(opart.getPartClass())]++;
            BucklePart npart = (BucklePart)item;
            if (npart.getX() == opart.getX() && npart.getY() == opart.getY()) {
                parts[ii] = null;
            } else if (Math.abs(npart.getX()) > AvatarLogic.BUCKLE_WIDTH / 2 ||
                    Math.abs(npart.getY()) > AvatarLogic.BUCKLE_HEIGHT / 2) {
                log.warning("Invalid buckle part coordinates [gang=" + this + ", handle=" +
                    handle + ", part=" + npart + "].");
                throw new InvocationException(INTERNAL_ERROR);
            } else {
                changed = true;
            }
        }
        if (!changed && Arrays.equals(partIds, _gangobj.buckle)) {
            // nothing changed, so we're finished
            listener.requestProcessed();
            return;
        }

        // verify that the part counts match the limits
        for (int ii = 0; ii < ccounts.length; ii++) {
            AvatarLogic.PartClass pclass = AvatarLogic.BUCKLE_PARTS[ii];
            if (!pclass.isOptional() && ccounts[ii] < 1) {
                log.warning("Buckle missing required part [gang=" + this + ", handle=" + handle +
                    ", parts=" + StringUtil.toString(parts) + ", pclass=" + pclass.name + "].");
                throw new InvocationException(INTERNAL_ERROR);
            }
            int max = (pclass.isMultiple() ? _gangobj.getMaxBuckleIcons() : 1);
            if (ccounts[ii] > max) {
                log.warning("Buckle has more than allowed number of parts [gang=" + this +
                    ", handle=" + handle + ", parts=" + StringUtil.toString(parts) + ", pclass=" +
                    pclass.name + ", max=" + max + "].");
                throw new InvocationException(INTERNAL_ERROR);
            }
        }

        // post the updates to the database
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                for (BucklePart part : parts) {
                    if (part != null) {
                        BangServer.itemrepo.updateItem(part);
                    }
                }
                BangServer.gangrepo.updateBuckle(_gangId, partIds, buckle.print);
                incLeaderLevel(_gangobj, handle);
            }
            public void handleSuccess () {
                GangMemberEntry leader = _gangobj.members.get(handle);
                _gangobj.startTransaction();
                try {
                    for (BucklePart part : parts) {
                        if (part != null) {
                            _gangobj.updateInventory(part);
                        }
                    }
                    _gangobj.setBuckle(partIds);
                    _gangobj.updateMembers(leader);
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
                    BangServer.gangrepo.recordDonation(entry.playerId, scrip, coins);
                    _entryId = BangServer.gangrepo.insertHistoryEntry(_gangId,
                        MessageBundle.compose(
                            "m.donation_entry",
                            MessageBundle.taint(handle),
                            GangUtil.getMoneyDesc(scrip, coins, 0)));
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
                BangServer.gangrepo.retractDonation(entry.playerId, scrip, coins);
                if (_entryId > 0) {
                    BangServer.gangrepo.deleteHistoryEntry(_entryId);
                }
            }

            protected void actionCompleted () {
                log.info("Added to gang coffers [gang=" + GangHandler.this + ", member=" + handle +
                         ", scrip=" + scrip + ", coins=" + coins + "].");
                GangMemberEntry member = _gangobj.members.get(handle);
                _gangobj.startTransaction();
                try {
                    _gangobj.setScrip(_gangobj.scrip + scrip);
                    _gangobj.setCoins(_gangobj.coins + coins);
                    if (member != null) {
                        member.scripDonated += scrip;
                        member.coinsDonated += coins;
                        _gangobj.updateMembers(member);
                    }
                } finally {
                    _gangobj.commitTransaction();
                }
                StringBuffer buf = (new StringBuffer("gang_transfer ")).append(_playerId);
                buf.append(" g:").append(_gangobj.gangId).append(" s:").append(scrip);
                buf.append(" c:").append(coins);
                BangServer.itemLog(buf.toString());
                super.actionCompleted();
            }

            protected String getGoodType () {
                return null;
            }

            protected int _entryId;
        }.start();
    }

    // documentation inherited from interface GangPeerProvider
    public void reserveScrip (
            ClientObject caller, final int scrip, final InvocationService.ResultListener listener)
        throws InvocationException
    {
        // make sure is comes from this server or a peer
        verifyLocalOrPeer(caller);

        // update the object to say the scrip is spent
        _gangobj.setScrip(_gangobj.scrip - scrip);

        // persist this expenditure to the database
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    BangServer.gangrepo.spendScrip(_gangId, scrip);
                } catch (PersistenceException pe) {
                    _error = pe;
                }
                return true;
            }

            public void handleResult () {
                if (_error == null) {
                    listener.requestProcessed(null);
                } else {
                    // return the scrip to the player object before failing
                    _gangobj.setScrip(_gangobj.scrip + scrip);
                    listener.requestFailed(_error.getMessage());
                }
            }

            protected PersistenceException _error;
        });
    }

    // documentation inherited from interface GangPeerProvider
    public void grantScrip (ClientObject caller, final int scrip)
    {
        try {
            // make sure is comes from this server or a peer
            verifyLocalOrPeer(caller);
        } catch (InvocationException ie) {
            log.warning("Grant scrip request received from illegal caller. " +
                    "[caller=" + caller + ", ie=" + ie + "].");
        }

        // persist this expenditure to the database
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    BangServer.gangrepo.grantScrip(_gangId, scrip);
                    return true;
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to grant scrip to gang " + "[id=" + _gangId +
                            ", amount=" + scrip + "].", pe);
                    return false;
                }
            }

            public void handleResult () {
                _gangobj.setScrip(_gangobj.scrip + scrip);
            }
        });
    }

    // documentation inherited from interface GangPeerProvider
    public void updateCoins (ClientObject caller)
    {
        try {
            // make sure is comes from this server or a peer
            verifyLocalOrPeer(caller);
        } catch (InvocationException ie) {
            log.warning("Update Coins request received from illegal caller. " +
                    "[caller=" + caller + ", ie=" + ie + "].");
        }

        BangServer.invoker.postUnit(new RepositoryUnit() {
            public void invokePersist () throws Exception {
                _coins = BangServer.coinmgr.getCoinRepository().getCoinCount(
                    _gangobj.getCoinAccount());
            }
            public void handleSuccess () {
                _gangobj.setCoins(_coins);
            }
            public void handleFailure (Exception err) {
                log.log(Level.WARNING, "Error updating gang coin count. [id=" +
                    _gangId + "].", err);
            }
            protected int _coins;
        });
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

        // delete the gang if they're the last member
        _deleting = (_gangobj.members.size() == 1);

        // figure out how much we have to give back (nothing if they leave of their own free will)
        int[] refund = entry.getDonationReimbursement();
        if (handle == null) {
            refund[0] = refund[1] = 0;
        }
        new GangFinancialAction(_gangobj, false, refund[0], refund[1], 0) {
            protected int getCoinType () {
                return CoinTransaction.GANG_MEMBER_REIMBURSEMENT;
            }
            protected String getCoinDescrip () {
                return "m.gang_member_reimbursement";
            }

            protected String persistentAction ()
                throws PersistenceException {
                if (_coinCost > 0) {
                    _playerAccount = BangServer.playrepo.getAccountName(entry.playerId);
                }
                if (_deleting) {
                    BangServer.gangrepo.deleteGang(_gangId);
                } else {
                    deleteFromGang(entry.playerId);
                    _entryId = BangServer.gangrepo.insertHistoryEntry(_gangId, (handle == null) ?
                        MessageBundle.tcompose("m.left_entry", entry.handle) :
                        MessageBundle.tcompose("m.expelled_entry", handle, entry.handle));
                    incLeaderLevel(_gangobj, handle);
                }
                return null;
            }
            protected void spendCash ()
                throws PersistenceException {
                super.spendCash();
                if (_scripCost > 0) {
                    BangServer.playrepo.grantScrip(entry.playerId, _scripCost);
                }
            }
            protected void grantCash ()
                throws PersistenceException {
                super.grantCash();
                if (_scripCost > 0) {
                    BangServer.playrepo.spendScrip(entry.playerId, _scripCost);
                }
            }
            protected boolean spendCoins (int reservationId)
                throws PersistenceException {
                return BangServer.coinmgr.getCoinRepository().transferCoins(
                    reservationId, _playerAccount, CoinTransaction.GANG_MEMBER_REIMBURSEMENT,
                    "m.gang_member_reimbursement", "m.gang_member_reimbursement");
            }
            protected void rollbackPersistentAction ()
                throws PersistenceException {
                if (_deleting) {
                    // this should never happen, because the only thing that can cause this is if
                    // we fail to transfer the coins, and we won't be transferring any coins to the
                    // last guy out the door
                    log.warning("Gang cannot be resurrected for rollback [gang=" +
                        GangHandler.this + ", leaver=" + target + "].");
                } else {
                    BangServer.gangrepo.insertMember(new GangMemberRecord(
                        entry.playerId, _gangId, entry.rank, entry.commandOrder, entry.leaderLevel,
                        entry.lastLeaderCommand, entry.joined, entry.notoriety, entry.scripDonated,
                        entry.coinsDonated, entry.title));
                }
                if (_entryId > 0) {
                    BangServer.gangrepo.deleteHistoryEntry(_entryId);
                }
            }

            protected void actionCompleted () {
                _gangobj.removeFromMembers(entry.handle);
                if (_deleting) {
                    // remove from Hideout directory and shut down immediately
                    BangServer.hideoutmgr.removeGang(_gangobj.name);
                    shutdown();
                } else {
                    maybeScheduleUnload();
                }
                if (handle != null) {
                    GangMemberEntry leader = _gangobj.members.get(handle);
                    _gangobj.updateMembers(leader);
                }
                listener.requestProcessed();
                StringBuffer buf = (new StringBuffer("gang_refund ")).append(entry.playerId);
                buf.append(" g:").append(_gangobj.gangId).append(" s:").append(_scripCost);
                buf.append(" c:").append(_coinCost);
                BangServer.itemLog(buf.toString());
                BangServer.playmgr.clearPosterInfoCache(entry.handle);
            }
            protected void actionFailed (String cause) {
                _deleting = false;
                listener.requestFailed(cause);
            }

            protected String getGoodType () {
                return null;
            }

            protected String _playerAccount;
            protected int _entryId;
        }.start();
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
            listener.requestProcessed();
            return;
        }

        // for leaders, the command order will be one more than the highest current order
        int highestOrder = -1;
        if (rank == LEADER_RANK) {
            for (GangMemberEntry member : _gangobj.members) {
                if (member.rank == LEADER_RANK) {
                    highestOrder = Math.max(highestOrder, member.commandOrder);
                }
            }
        }
        final int commandOrder = highestOrder + 1;

        // post to the database
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                BangServer.gangrepo.updateRank(entry.playerId, rank, commandOrder);
                BangServer.gangrepo.insertHistoryEntry(_gangId, (handle == null) ?
                    MessageBundle.tcompose("m.auto_promotion_entry", target) :
                    MessageBundle.compose(
                        (entry.rank < rank) ? "m.promotion_entry" : "m.demotion_entry",
                        MessageBundle.taint(handle),
                        MessageBundle.taint(target),
                        MessageBundle.qualify(GANG_MSGS, XLATE_RANKS[rank])));
                incLeaderLevel(_gangobj, handle);
            }
            public void handleSuccess () {
                GangMemberEntry member = _gangobj.members.get(target);
                member.rank = rank;
                member.commandOrder = commandOrder;
                member.leaderLevel = 0;
                member.lastLeaderCommand = System.currentTimeMillis();
                _gangobj.updateMembers(member);
                if (handle != null) {
                    GangMemberEntry leader = _gangobj.members.get(handle);
                    _gangobj.updateMembers(leader);
                }
                listener.requestProcessed();
                BangServer.playmgr.clearPosterInfoCache(entry.handle);
            }
        });
    }

    // documentation inherited from interface GangPeerProvider
    public void changeMemberTitle (
        ClientObject caller, final Handle handle, final Handle target, final int title,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure they can change it
        final GangMemberEntry entry = verifyCanChange(handle, target);

        // make sure it's not the same rank
        if (title == entry.title) {
            listener.requestProcessed();
            return;
        }

        // post to the database
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                BangServer.gangrepo.updateTitle(entry.playerId, title);
                BangServer.gangrepo.insertHistoryEntry(_gangId,
                    MessageBundle.compose("m.title_entry",
                        MessageBundle.taint(handle),
                        MessageBundle.taint(target),
                        MessageBundle.qualify(GANG_MSGS, "m.title." + title)));
                incLeaderLevel(_gangobj, handle);
            }
            public void handleSuccess () {
                GangMemberEntry member = _gangobj.members.get(target);
                member.title = title;
                _gangobj.updateMembers(member);
                GangMemberEntry leader = _gangobj.members.get(handle);
                _gangobj.updateMembers(leader);
                listener.requestProcessed();
                BangServer.playmgr.clearPosterInfoCache(entry.handle);
            }
        });
    }

    // documentation inherited from interface GangPeerProvider
    public void getUpgradeQuote (ClientObject caller, Handle handle, GangGood good,
            InvocationService.ResultListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure it comes from a leader
        verifyIsLeader(handle);

        if (!(good instanceof WeightClassUpgradeGood)) {
            throw new InvocationException(INTERNAL_ERROR);
        }

        listener.requestProcessed(BangServer.hideoutmgr.upgradeCost(
                _gangobj, ((WeightClassUpgradeGood)good).getWeightClass()));
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
            if (catarts[ii] == null || catarts[ii].qualifier != null ||
                    catarts[ii].start != null || catarts[ii].stop != null) {
                log.warning("Invalid article requested for outfit [who=" + handle +
                    ", article=" + oart.article + "].");
                throw new InvocationException(INTERNAL_ERROR);
            }
            articles[ii] = BangServer.alogic.createArticle(-1, catarts[ii], oart.zations);
            if (articles[ii] == null) {
                // an error will have already been logged
                throw new InvocationException(INTERNAL_ERROR);
            }
            articles[ii].setGangId(_gangId);
        }

        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                // save the outfit as the gang's current
                BangServer.gangrepo.updateOutfit(_gangId, outfit);

                // find out who needs the articles and how much it will cost (we check against the
                // non-gang version of the article, too, because there's no point in granting a
                // limited article to someone who has an unlimited one)
                ArrayIntSet maleIds = new ArrayIntSet(), femaleIds = new ArrayIntSet();
                BangServer.gangrepo.loadMemberIds(_gangId, maleIds, femaleIds);
                for (int ii = 0; ii < articles.length; ii++) {
                    Article article = articles[ii],
                        alternate = (Article)article.clone();
                    alternate.setGangId(0);
                    ArrayIntSet memberIds = (article.getArticleName().indexOf("female") == -1) ?
                        maleIds : femaleIds;
                    ArrayIntSet ownerIds = BangServer.itemrepo.getItemOwners(
                        memberIds, article, alternate);
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

    // documentation inherited from interface GangPeerProvider
    public void rentGangGood (
        ClientObject caller, Handle handle, String type, Object[] args,
        boolean admin, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure it comes from a leader
        verifyIsLeader(handle);

        // create and start up the provider
        GangGoodProvider provider = BangServer.hideoutmgr.getRentalGoodProvider(
            this, handle, admin, type, args);
        provider.setListener(listener);
        provider.start();
    }

    // documentation inherited from interface GangPeerProvider
    public void renewGangItem (
        ClientObject caller, Handle handle, int itemId, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure is comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure it comes from a leader
        verifyIsLeader(handle);

        Item item = null;
        for (Item i : _gangobj.inventory) {
            if (i.getItemId() == itemId) {
                item = i;
                break;
            }
        }

        if (item == null) {
            throw new InvocationException(INTERNAL_ERROR);
        }

        RentalGood good = BangServer.hideoutmgr.getRentalGood(item);
        if (good == null) {
            throw new InvocationException(INTERNAL_ERROR);
        }

        ItemRenewal renewal = new ItemRenewal(_gangobj, handle, item, good);
        renewal.setListener(listener);
        renewal.start();
    }

    // documentation inherited from interface GangPeerProvider
    public void buyGangGood (
        ClientObject caller, Handle handle, String type, Object[] args,
        boolean admin, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure it comes from a leader
        verifyIsLeader(handle);

        // create and start up the provider
        GangGoodProvider provider = BangServer.hideoutmgr.getGoodProvider(
            this, handle, admin, type, args);
        provider.setListener(listener);
        provider.start();
    }

    // documentation inherited from interface GangPeerProvider
    public void broadcastToMembers (
        ClientObject caller, Handle handle, String message,
        InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it comes from this server or a peer
        verifyLocalOrPeer(caller);

        // make sure it comes from a leader
        verifyIsLeader(handle);

        // transmit on the gang object and report success
        SpeakUtil.sendSpeak(_gangobj, handle, null, message, ChatCodes.BROADCAST_MODE);
        listener.requestProcessed();
    }

    // documentation inherited from interface GangPeerProvider
    public void tradeCompleted (
            ClientObject caller, final int price, final int vol, final String member)
    {
        try {
            // make sure it comes from this server or a peer
            verifyLocalOrPeer(caller);
        } catch (InvocationException ie) {
            log.warning("Failed to log completed trade. [ie=" + ie + "].");
            return;
        }

        // log the completed trade in the gang history
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    BangServer.gangrepo.insertHistoryEntry(_gangId,
                        MessageBundle.tcompose(
                            "m.exchange_purchase_entry", member, "" + vol, "" + price*vol));
                    incLeaderLevel(_gangobj, new Handle(member));
                } catch (PersistenceException pe) {
                    _error = pe;
                }
                return true;
            }

            public void handleResult () {
                if (_error != null) {
                    log.warning("Failed to log completed trade. [error=" + _error + "].");
                } else {
                    GangMemberEntry leader = _gangobj.members.get(new Handle(member));
                    _gangobj.updateMembers(leader);
                }
            }

            protected PersistenceException _error;
        });
    }

    /**
     * Distributes rented items to online members.
     */
    protected void distributeRental (Item item)
    {
        final ArrayIntSet userIds = new ArrayIntSet();
        boolean maleItem = true;
        final Item citem = (Item)item.clone();
        for (GangMemberEntry member : _gangobj.members) {
            PlayerObject user = BangServer.lookupPlayer(member.playerId);
            if (user != null && item.canBeOwned(user) && !user.holdsEquivalentItem(citem)) {
                userIds.add(user.playerId);
            }
        }
        if (userIds.isEmpty()) {
            return;
        }
        citem.setGangId(_gangobj.gangId);
        citem.setGangOwned(false);
        BangServer.invoker.postUnit(
            new RepositoryUnit("distributeRental") {
                public void invokePersist () throws PersistenceException {
                    BangServer.itemrepo.insertItems(citem, userIds, _items);
                }
                public void handleSuccess () {
                    for (Item item : _items) {
                        PlayerObject user = BangServer.lookupPlayer(item.getOwnerId());
                        if (user != null) {
                            user.addToInventory(item);
                        }
                    }
                }
                public void handleFailure (Exception cause) {
                }
                protected ArrayList<Item> _items = new ArrayList<Item>();
            });
    }

    /**
     * Gives a new gang member the gang rented items.
     */
    protected void deliverGangItems (final PlayerObject user)
    {
        final ArrayList<Item> added = new ArrayList<Item>();
        long now = System.currentTimeMillis();
        for (Item item : _gangobj.inventory) {
            if (item.canBeOwned(user) && !user.holdsEquivalentItem(item) && !item.isExpired(now)) {
                Item citem = (Item)item.clone();
                citem.setGangId(citem.getOwnerId());
                citem.setOwnerId(user.playerId);
                citem.setGangOwned(false);
                citem.setItemId(0);
                added.add(citem);
            }
        }

        if (added.isEmpty()) {
            return;
        }

        BangServer.invoker.postUnit(new RepositoryUnit("deliverGangItems") {
            public void invokePersist ()
                throws PersistenceException {
                for (Item item : added) {
                    BangServer.itemrepo.insertItem(item);
                }
            }
            public void handleSuccess () {
                user.startTransaction();
                try {
                    for (Item item : added) {
                        user.addToInventory(item);
                    }
                } finally {
                    user.commitTransaction();
                }
            }
            public void handleFailure (Exception cause) {
                log.warning("Failed to deliver gang items to new member [gang=" + GangHandler.this +
                    ", user=" + user.who() + ", error=" + cause + "].");
            }
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
        new GangFinancialAction(_gangobj, admin, scripCost, coinCost, 0) {
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
                            GangUtil.getMoneyDesc(scripCost, coinCost, 0),
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
                super.actionCompleted();
            }
            protected void actionFailed (String cause) {
                listener.requestFailed(cause);
            }

            protected String getGoodType () {
                return "Outfits";
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
        if (LEADER_LEVEL_WAITS[entry.leaderLevel] > 0 &&
                !_gangobj.getSeniorLeader().handle.equals(entry.handle)) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, -LEADER_LEVEL_WAITS[entry.leaderLevel]);
            int remain = (int)Math.ceil(
                    (entry.lastLeaderCommand - cal.getTimeInMillis())/(double)ONE_HOUR);
            if (remain > 0) {
                throw new InvocationException(MessageBundle.qualify(
                    HideoutCodes.HIDEOUT_MSGS, MessageBundle.tcompose(NEW_LEADER_WAIT, remain)));
            }
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

                    if (_grec == null) {
                        return;
                    }

                    // TEMP: validate the buckle and weight class, fixing as necessary
                    validateBuckle(_grec);
                    validateWeightClass(_grec);

                    // check for expired items
                    long now = System.currentTimeMillis();
                    Item[] items = _grec.inventory.toArray(new Item[_grec.inventory.size()]);
                    ArrayIntSet removals = new ArrayIntSet();
                    for (Item item : items) {
                        if (item.isExpired(now) || ((item instanceof WeightClassUpgrade) &&
                                ((WeightClassUpgrade)item).getWeightClass() != _grec.weightClass)) {
                            removals.add(item.getItemId());
                            _grec.inventory.remove(item);
                        }
                    }
                    if (!removals.isEmpty()) {
                        BangServer.itemrepo.deleteItems(removals, "Gang item expired");
                    }
                }
                public void handleSuccess () {
                    if (_grec == null) {
                        initFailed(new Exception("No such gang"));
                    } else {
                        createGangObject(_grec);
                    }
                }
                public void handleFailure (Exception cause) {
                    initFailed(cause);
                }
                protected GangRecord _grec;
            });
    }

    /**
     * Makes sure the gang's buckle matches its inventory.  This is run on the invoker thread.
     */
    protected void validateBuckle (GangRecord record)
        throws PersistenceException
    {
        int[] buckle = record.getBuckle();
        if (buckle.length < 2) {
            recreateBuckle(record);
            return;
        }

        DSet<Item> inventory = new DSet<Item>(record.inventory);
        boolean update = false;
        for (int ii = 0; ii < buckle.length; ii++) {
            Item item = inventory.get(buckle[ii]);
            if (!(item instanceof BucklePart)) {
                recreateBuckle(record);
                return;
            }
            BucklePart part = (BucklePart)item;
            if ((ii == 0 && !"background".equals(part.getPartClass())) ||
                (ii == 1 && !"border".equals(part.getPartClass())) ||
                (ii >= 2 && !"icon".equals(part.getPartClass()))) {
                recreateBuckle(record);
                return;
            }
        }
    }

    /**
     * Recreates the gang's buckle from what they already have (if anything).
     */
    protected void recreateBuckle (GangRecord record)
        throws PersistenceException
    {
        log.info("Recreating invalid buckle [gang=" + this + ", buckle=" +
            StringUtil.toString(record.getBuckle()) + ", inventory=" + record.inventory + "].");
        int[] buckle = new int[2];
        BucklePart[] parts = new BucklePart[2];
        for (Item item : record.inventory) {
            if (!(item instanceof BucklePart)) {
                continue;
            }
            BucklePart part = (BucklePart)item;
            if ("background".equals(part.getPartClass()) && parts[0] == null) {
                parts[0] = part;
                buckle[0] = part.getItemId();
            } else if ("border".equals(part.getPartClass()) && parts[1] == null) {
                parts[1] = part;
                buckle[1] = part.getItemId();
            }
            if (parts[0] != null && parts[1] != null) {
                break;
            }
        }
        if (parts[0] == null || parts[1] == null) {
            BucklePart[] dparts = BangServer.alogic.createDefaultBuckle();
            for (int ii = 0; ii < 2; ii++) {
                if (parts[ii] != null) {
                    continue;
                }
                parts[ii] = dparts[ii];
                parts[ii].setOwnerId(record.gangId);
                BangServer.itemrepo.insertItem(parts[ii]);
                record.inventory.add(parts[ii]);
                buckle[ii] = parts[ii].getItemId();
            }
        }

        BuckleInfo info = GangUtil.getBuckleInfo(parts);
        BangServer.gangrepo.updateBuckle(record.gangId, buckle, info.print);
        record.setBuckle(buckle, info.print);
    }

    /**
     * Makes sure the cached weight class matches the inventory.
     */
    protected void validateWeightClass (GangRecord record)
        throws PersistenceException
    {
        byte weightClass = GangUtil.getWeightClass(record.inventory);
        if (record.weightClass == weightClass) {
            return;
        }
        log.info("Correcting weight class [gang=" + this + ", weightClass = " + weightClass +
            ", inventory=" + record.inventory + "].");
        BangServer.gangrepo.updateWeightClass(record.gangId, weightClass);
        record.weightClass = weightClass;
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
        _gangobj.aces = record.aces;
        _gangobj.buckle = record.getBuckle();
        _gangobj.outfit = record.outfit;
        _gangobj.members = new DSet<GangMemberEntry>(record.members.iterator());

        _gangobj.inventory = new DSet<Item>(record.inventory);

        _notoriety = record.notoriety;
        _gangobj.notoriety = GangUtil.getNotorietyLevel(_gangobj.getWeightClass(), _notoriety);

        _gangobj.rentMultiplier = RuntimeConfig.server.rentMultiplier[_gangobj.getWeightClass()];
        _gangobj.articleRentMultiplier = RuntimeConfig.server.articleRentMultiplier[
            _gangobj.getWeightClass()];
        RuntimeConfig.server.addListener(new AttributeChangeListener() {
            public void attributeChanged (AttributeChangedEvent event)
            {
                String name = event.getName();
                if (name.equals(ServerConfigObject.RENT_MULTIPLIER)) {
                    _gangobj.setRentMultiplier(
                        RuntimeConfig.server.rentMultiplier[_gangobj.getWeightClass()]);
                } else if (name.equals(ServerConfigObject.ARTICLE_RENT_MULTIPLIER)) {
                    _gangobj.setArticleRentMultiplier(
                        RuntimeConfig.server.articleRentMultiplier[_gangobj.getWeightClass()]);
                }
            }
        });

        // the avatar id is that of the senior leader
        GangMemberEntry leader = _gangobj.getSeniorLeader();
        _avatarId = (leader == null) ? 0 : leader.playerId;

        // find out which members are online (and listen for updates)
        for (GangMemberEntry entry : _gangobj.members) {
            initTownIndex(entry);
        }
        BangServer.addPlayerObserver(this);
        if (BangServer.peermgr != null) {
            BangServer.peermgr.addPlayerObserver(this);
            BangServer.peermgr.addDroppedLockObserver(this);
        }

        // register the service for peers
        _gangobj.gangPeerService =
            (GangPeerMarshaller)BangServer.invmgr.registerDispatcher(
                new GangPeerDispatcher(this));

        // register the speak service for local users
        _gangobj.speakService = (SpeakMarshaller)BangServer.invmgr.registerDispatcher(
            new SpeakDispatcher(new SpeakHandler(_gangobj, this)));

        // create our table game manager for this town
        _tmgr = new TableGameManager();
        _gangobj.tableOid = _tmgr.getTableGameObject().getOid();

        // register and announce
        BangServer.omgr.registerObject(_gangobj);
        log.info("Initialized gang object " + this + ".");

        // start the interval to calculate top-ranked members
        _rankval = new Interval(BangServer.omgr) {
            public void expired () {
                SaloonManager.refreshTopRanked(
                    _gangobj, ScenarioInfo.getScenarioIds(),
                    "GANG_MEMBERS", "RATINGS.PLAYER_ID = GANG_MEMBERS.PLAYER_ID and " +
                    "GANG_MEMBERS.GANG_ID = " + _gangId, TOP_RANKED_LIST_SIZE);
            }
        };
        _rankval.schedule(1000L, RANK_REFRESH_INTERVAL);

        // and the one to update activity states
        _actival = new Interval(BangServer.omgr) {
            public void expired () {
                updateActivity();
            }
        };
        _actival.schedule(1000L, ACTIVITY_INTERVAL);

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
     * Updates the members' active states, which may change the senior leader or trigger an
     * auto-promotion.
     */
    protected void updateActivity ()
    {
        // find out who's inactive
        ArrayList<GangMemberEntry> updates = new ArrayList<GangMemberEntry>();
        for (GangMemberEntry entry : _gangobj.members) {
            boolean oactive = entry.wasActive;
            entry.updateWasActive();
            if (entry.wasActive != oactive) {
                updates.add(entry);
            }
        }

        // update in the dobj
        if (!updates.isEmpty()) {
            _gangobj.startTransaction();
            try {
                for (GangMemberEntry entry : updates) {
                    _gangobj.updateMembers(entry);
                }
            } finally {
                _gangobj.commitTransaction();
            }
        }

        // check for auto-promotions, avatar updates
        maybeAutoPromote();
        updateAvatar(null);
    }

    /**
     * If there are no active leaders, promotes the most senior active member.
     */
    protected void maybeAutoPromote ()
    {
        // stop if we are not the master or are already auto-promoting
        if (_client != null || _promoting) {
            return;
        }

        // find the most notorious active member (breaking ties by seniority),
        // returning if we find an active leader
        GangMemberEntry senior = null;
        for (GangMemberEntry entry : _gangobj.members) {
            if (!entry.isActive()) {
                continue;
            }
            if (entry.rank == LEADER_RANK) {
                return;
            } else if (senior == null || entry.notoriety > senior.notoriety ||
                (entry.notoriety == senior.notoriety && entry.joined < senior.joined)) {
                senior = entry;
            }
        }
        if (senior == null) {
            // if there's no senior member to auto-promote, we will end up promoting the
            // first one who logs on (unless it's a leader)
            return;
        }

        // perform the auto-promotion
        final Handle handle = senior.handle;
        _promoting = true;
        try {
            changeMemberRank(null, null, handle, LEADER_RANK,
                new InvocationService.ConfirmListener() {
                    public void requestProcessed () {
                        log.info("Automatically promoted senior member due to lack of active " +
                            "leaders [gang=" + this + ", member=" + handle + "].");
                        _promoting = false;
                    }
                    public void requestFailed (String cause) {
                        _promoting = false;
                    }
                });
        } catch (InvocationException e) {
            _promoting = false;
        }
    }

    /**
     * Called when a player logs in or out of a town server.
     */
    protected void playerLocationChanged (Name name, int oldTownIndex, int newTownIndex)
    {
        GangMemberEntry entry = (name == null) ? null : _gangobj.members.get(name);
        if (entry == null || (newTownIndex == -1 && entry.townIdx != oldTownIndex)) {
            // when moving between towns, make sure that we don't count the "logging off"
            // of the old town after moving to the new
            return;
        }
        if ((entry.townIdx = (byte)newTownIndex) == -1) {
            entry.wasActive = true;
            entry.lastSession = System.currentTimeMillis();
        }
        _gangobj.updateMembers(entry);
        if (newTownIndex == -1) {
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
        // if there are no peers, shut down immediately
        if (BangServer.peermgr == null) {
            shutdown();
            return;
        }

        // otherwise, attempt to drop the lock
        BangServer.peermgr.releaseLock(_releasing = new Lock("gang", _gangId),
            new ResultListener<String>() {
                public void requestCompleted (String result) {
                    _releasing = null;
                    if (ServerConfig.nodename.equals(result)) {
                        maybeScheduleUnload();
                    } else {
                        shutdown();
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
                    setGangObject(result);
                }
                public void requestFailed (Exception cause) {
                    initFailed(cause);
                }
            });
    }

    /**
     * Configures a gang object received from a peer node.
     */
    protected void setGangObject (GangObject gangobj)
    {
        _gangobj = gangobj;

        // rewrite the speak service with a provider of our own that forwards speech to
        // the controlling node
        _gangobj.speakService =
            (SpeakMarshaller)BangServer.invmgr.registerDispatcher(new SpeakDispatcher(
                new SpeakProvider() {
                    public void speak (ClientObject caller, String message, byte mode) {
                        _gangobj.gangPeerService.sendSpeak(
                            _client, ((PlayerObject)caller).handle, message, mode);
                    }
                }));

        // create our local TableGameManager
        _tmgr = new TableGameManager();
        _gangobj.tableOid = _tmgr.getTableGameObject().getOid();

        _proxy = PeerUtil.createProviderProxy(
            GangPeerProvider.class, _gangobj.gangPeerService, _client);

        _client.addClientObserver(_unsubber = new ClientAdapter() {
            @Override // documentation inherited
            public void clientDidLogoff (Client client) {
                unsubscribeFromPeer();
            }
        });

        log.info("Subscribed to gang " + this + " on " + _nodeName + ".");
        didInit();
    }

    /**
     * Notifies the listeners with the newly acquired gang object.
     */
    protected void didInit ()
    {
        // initialize any players already online
        GangMemberEntry[] members = _gangobj.members.toArray(
            new GangMemberEntry[_gangobj.members.size()]);
        for (GangMemberEntry member : members) {
            PlayerObject user = BangServer.lookupPlayer(member.playerId);
            if (user != null) {
                initPlayer(user, member);
            }
        }
        _gangobj.addListener(this);
        _gangobj.addListener(BangServer.playmgr.receivedChatListener);
        BangServer.gangmgr.mapGang(_gangobj.name, this);
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
        BangServer.gangmgr.unmapGang(_gangId, null);
    }

    /**
     * Unsubscribes from the peer and unmaps the gang.
     */
    protected void unsubscribeFromPeer ()
    {
        _client.removeClientObserver(_unsubber);
        _gangobj.removeListener(this);
        _gangobj.removeListener(BangServer.playmgr.receivedChatListener);
        BangServer.invmgr.clearDispatcher(_gangobj.speakService);
        BangServer.peermgr.unproxyRemoteObject(_nodeName, _gangobj.remoteOid);

        log.info("Unsubscribed from gang " + this + ".");
        BangServer.gangmgr.unmapGang(_gangId, _gangobj.name);

        // if there are members still online, we will have to re-resolve; hopefully
        // this will happen so quickly that no one will notice
        for (GangMemberEntry member : _gangobj.members) {
            if (BangServer.lookupPlayer(member.playerId) != null) {
                log.warning("Proxied gang vanished while members still online [gang=" +
                    this + "].");
                BangServer.gangmgr.resolveGang(_gangId);
                return;
            }
        }
    }

    /**
     * Shuts the gang down and unmaps it.
     */
    protected void shutdown ()
    {
        _rankval.cancel();
        _actival.cancel();
        _unloadval.cancel();

        BangServer.removePlayerObserver(this);
        if (BangServer.peermgr != null) {
            BangServer.peermgr.removePlayerObserver(this);
            BangServer.peermgr.removeDroppedLockObserver(this);
        }

        if (_tmgr != null) {
            _tmgr.shutdown();
            _tmgr = null;
        }

        BangServer.invmgr.clearDispatcher(_gangobj.gangPeerService);
        BangServer.invmgr.clearDispatcher(_gangobj.speakService);
        _gangobj.destroy();

        log.info("Gang shutdown " + this + ".");
        BangServer.gangmgr.unmapGang(_gangId, _gangobj.name);
    }

    /**
     * Updates the senior leader's avatar, tracking it over time if he's online.
     *
     * @param player if non-null, a candidate player object in the process of resolution.
     */
    protected void updateAvatar (PlayerObject player)
    {
        final GangMemberEntry leader = _gangobj.getSeniorLeader();
        PlayerObject user = (leader == null) ? null :
            ((player != null && player.playerId == leader.playerId) ?
                player : BangServer.lookupPlayer(leader.playerId));
        if (user != null) {
            if (_avupdater == null || _avatarId != user.playerId) {
                if (_avupdater != null) {
                    _avupdater.remove();
                }
                (_avupdater = new AvatarUpdater(this)).add(user);
                _avatarId = user.playerId;
            }
            return;
        }
        if (_avupdater != null) {
            _avupdater.remove();
            _avupdater = null;
        }
        if (_client != null || leader == null || _avatarId == leader.playerId ||
            (BangServer.peermgr != null &&
                BangServer.peermgr.locateRemotePlayer(leader.handle) != null)) {
            return;
        }
        _avatarId = leader.playerId;
        BangServer.invoker.postUnit(new RepositoryUnit() {
            public void invokePersist () throws PersistenceException {
                _avatar = BangServer.lookrepo.loadSnapshot(leader.playerId);
            }
            public void handleSuccess () {
                _gangobj.setAvatar(_avatar);
            }
            public void handleFailure (Exception cause) {
                log.warning("Failed to load senior leader avatar [gang=" + GangHandler.this +
                    ", leader=" + leader + ", error=" + cause + "].");
            }
            protected AvatarInfo _avatar;
        });
    }

    /**
     * Called any time the gang state visible on the gang info page is changed.
     */
    protected void gangInfoChanged ()
    {
        // the master server will broadcast the update to its peers
        if (_client == null) {
            BangServer.gangmgr.clearCachedGangInfo(_gangobj.name);
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
            SpeakUtil.sendInfo(invobj, GANG_MSGS,
                MessageBundle.tcompose(
                    (mrec == null) ? "m.member_rejected" : "m.member_accepted",
                    handle, _gangobj.name));
        }
        if (mrec != null) {
            GangMemberEntry entry = new GangMemberEntry(handle, playerId, MEMBER_RANK, 0, 0,
                    mrec.lastLeaderCommand, mrec.joined, 0, 0, 0, 0, mrec.joined);
            initTownIndex(entry);
            _gangobj.addToMembers(entry);
            if (entry.townIdx == -1) {
                maybeScheduleUnload();
            } else {
                maybeCancelUnload();
            }
        }
        listener.requestProcessed();
        BangServer.generalLog("joined_gang " + playerId + " g:" + _gangobj.gangId);
    }

    /**
     * Sets the town index for the supplied member entry based on where the member is
     * logged in (if anywhere).
     */
    protected void initTownIndex (GangMemberEntry entry)
    {
        PlayerObject user = BangServer.lookupPlayer(entry.playerId);
        if (user != null) {
            entry.townIdx = (byte)ServerConfig.townIndex;
            return;
        }
        Tuple<BangClientInfo, Integer> result = (BangServer.peermgr == null) ?
            null : BangServer.peermgr.locateRemotePlayer(entry.handle);
        entry.townIdx = (result == null) ? -1 : result.right.byteValue();
    }

    /**
     * Removes the specified member and strips his looks of any gang articles.
     */
    protected void deleteFromGang (int playerId)
        throws PersistenceException
    {
        // delete the member record itself
        BangServer.gangrepo.deleteMember(playerId);

        ArrayIntSet removals = new ArrayIntSet();
        ArrayList<Look> modified = stripLooks(BangServer.itemrepo.loadItems(playerId),
                BangServer.lookrepo.loadLooks(playerId), removals);

        for (Look look : modified) {
            BangServer.lookrepo.updateLook(playerId, look);
        }
        if (!removals.isEmpty()) {
            BangServer.itemrepo.deleteItems(removals, "Booted from gang");
        }
    }

    /**
     * Returns a list of modified looks with all gang owned articles removed.
     */
    protected ArrayList<Look> stripLooks (
            Iterable<Item> items, Iterable<Look> looks, ArrayIntSet removals)
    {
        for (Item item : items) {
            if (item.getGangId() == _gangId) {
                removals.add(item.getItemId());
            }
        }

        // strip the looks of all gang articles and update
        return AvatarLogic.stripLooks(removals, items, looks);
    }

    /** The id of our gang. */
    protected int _gangId;

    /** Listeners for the gang object. */
    protected ResultListenerList<GangObject> _listeners = new ResultListenerList<GangObject>();

    /** The name of the node controlling the gang object. */
    protected String _nodeName;

    /** The peer client, if our gang object is a proxy for a remote one. */
    protected Client _client;

    /** Unsubscribes from the gang object when the peer connection is lost. */
    protected ClientAdapter _unsubber;

    /** The proxy object implementing {@link GangPeerProvider} to forward requests to the peer. */
    protected GangPeerProvider _proxy;

    /** The gang object, when resolved. */
    protected GangObject _gangobj;

    /** The table game manager for this gang/town. */
    protected TableGameManager _tmgr;

    /** The gang's raw notoriety. */
    protected int _notoriety;

    /** The player id of the avatar set in the gang object. */
    protected int _avatarId;

    /** The interval that refreshes the list of top-ranked members. */
    protected Interval _rankval;

    /** The interval that refreshes members' active states. */
    protected Interval _actival;

    /** The interval that starts the unloading process. */
    protected Interval _unloadval;

    /** If non-null, we're trying to release this lock. */
    protected Lock _releasing;

    /** Keeps the gang's senior leader avatar up-to-date. */
    protected AvatarUpdater _avupdater;

    /** Set when auto-promoting to keep us from doing it more than once. */
    protected boolean _promoting;

    /** Set when deleting the gang to prevent us from accepting any new members. */
    protected boolean _deleting;

    /** Stores the player ids of users in the process of leaving the gang voluntarily. */
    protected ArrayIntSet _leaverIds = new ArrayIntSet();

    /** The frequency with which we update the top-ranked member lists. */
    protected static final long RANK_REFRESH_INTERVAL = 60 * 60 * 1000L;

    /** The size of the top-ranked member lists. */
    protected static final int TOP_RANKED_LIST_SIZE = 10;

    /** The frequency with which we update members' active states. */
    protected static final long ACTIVITY_INTERVAL = 3 * 60 * 60 * 1000L;

    /** In order to prevent rapid loading and unloading, we wait this long after the last gang
     * member has logged off of the cluster to unload the gang. */
    protected static final long UNLOAD_INTERVAL = 60 * 60 * 1000L;

    /** One hour in milliseconds. */
    protected static final long ONE_HOUR = 60 * 60 * 1000L;

}
