//
// $Id$

package com.threerings.bang.gang.server;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.jdbc.RepositoryUnit;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SoftCache;

import com.threerings.io.Streamable;
import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.PosterInfo;
import com.threerings.bang.server.BangInvoker;
import com.threerings.bang.server.BangPeerManager;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.FinancialAction;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PlayerRecord;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.gang.client.GangService;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangEntry;
import com.threerings.bang.gang.data.GangInfo;
import com.threerings.bang.gang.data.GangInvite;
import com.threerings.bang.gang.data.GangMarshaller;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.server.persist.GangInviteRecord;
import com.threerings.bang.gang.server.persist.GangMemberRecord;
import com.threerings.bang.gang.server.persist.GangRecord;
import com.threerings.bang.gang.server.persist.GangRepository;
import com.threerings.bang.gang.util.GangUtil;

import static com.threerings.bang.Log.*;

/**
 * Handles gang-related functionality.
 */
@Singleton
public class GangManager
    implements GangProvider, GangCodes
{
    /**
     * Initializes the gang manager and registers its invocation service.
     */
    public void init ()
        throws PersistenceException
    {
        // register ourselves as the provider of the (bootstrap) GangService
        BangServer.invmgr.registerProvider(this, GangMarshaller.class, GLOBAL_GROUP);

        // listen for gang info cache updates
        if (_peermgr.isRunning()) {
            _peermgr.addStaleCacheObserver(GANG_INFO_CACHE,
                new PeerManager.StaleCacheObserver() {
                    public void changedCacheData (Streamable data) {
                        _infoCache.remove((Handle)data);
                    }
                });
        }

        // listen for gang notoriety updates
        if (_peermgr.isRunning()) {
            _peermgr.addStaleCacheObserver(GANG_NOTORIETY_CACHE,
                new PeerManager.StaleCacheObserver() {
                    public void changedCacheData (Streamable data) {
                        syncNotoriety();
                    }
                });
        }

        // if we're in Frontier Town, start our errosion interval to run at 2am every day
        if (BangCodes.FRONTIER_TOWN.equals(ServerConfig.townId)) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 2);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            long delay = cal.getTimeInMillis() - System.currentTimeMillis();
            if (delay < 0) {
                delay += ERODE_NOTORIETY_INTERVAL;
            }
            new Interval (Interval.RUN_DIRECT) {
                public void expired () {
                    erodeNotoriety();
                }
            }.schedule(delay, ERODE_NOTORIETY_INTERVAL);
        }

    }

    /**
     * Resolves the specified gang, either loading it from the database (if it has not been
     * loaded on any other servers), or subscribing to it through another server, and
     * provides the {@link GangObject} to the provided listener on completion.
     */
    public void resolveGang (int gangId, ResultListener<GangObject> listener)
    {
        resolveGang(gangId).getGangObject(listener);
    }

    /**
     * Resolves the specified gang.
     */
    public GangHandler resolveGang (int gangId)
    {
        GangHandler gang = _gangs.get(gangId);
        if (gang == null) {
            _gangs.put(gangId, gang = GangHandler.newGangHandler(_injector, gangId));
        }
        return gang;
    }

    /**
     * Registers a just-logged-on player with their Gang, if appropriate, dispatches any pending
     * Gang invitations otherwise. Called during logon after a player has resolved all of their
     * persistent data.
     */
    public void initPlayer (
        PlayerObject player, GangMemberRecord mrec, List<GangInviteRecord> invites)
    {
        BangServer.requireDObjThread(); // safety first

        // if they're a gang member, wire them up; otherwise, dispatch any pending gang invitations
        if (mrec != null) {
            try {
                requireGang(mrec.gangId).initPlayer(player);
            } catch (InvocationException e) {
                log.warning("Gang not loaded to init player", "gangId", mrec.gangId,
                            "player", player.who());
            }
        } else if (invites != null) {
            for (GangInviteRecord record : invites) {
                sendGangInviteLocal(player, record.inviter, record.gangId, record.name,
                    record.message);
            }
        }
    }

    /**
     * Populates a gang-related player info.
     */
    public void populatePlayerInfo (BangObject.PlayerInfo pinfo, PlayerObject player)
    {
        if (player.gangId <= 0) {
            log.info("Can't populate player info", "gangId", player.gangId);
            return;
        }
        try {
            GangObject gangobj = requireGang(player.gangId).getGangObject();
            pinfo.gang = gangobj.name;
            pinfo.buckle = gangobj.getBuckleInfo();
        } catch (InvocationException e) {
            log.warning("Gang not loaded to populate player info", "gangId", player.gangId,
                        "player", player.who());
        }
    }

    /**
     * Populates the gang-related poster fields for an online player.
     */
    public void populatePosterInfo (PosterInfo info, PlayerObject player)
    {
        if (player.gangId <= 0) {
            return;
        }
        try {
            GangObject gangobj = requireGang(player.gangId).getGangObject();
            info.gang = gangobj.name;
            info.rank = player.gangRank;
            info.title = player.gangTitle;
            info.buckle = gangobj.getBuckleInfo();
        } catch (InvocationException e) {
            log.warning("Gang not loaded to populate poster", "gangId", player.gangId,
                        "player", player.who());
        }
    }

    /**
     * Populates the gang-related poster fields for an offline player (from the invoker thread).
     */
    public void populatePosterInfo (PosterInfo info, PlayerRecord player)
        throws PersistenceException
    {
        BangServer.refuseDObjThread(); // safety first
        GangMemberRecord mrec = _gangrepo.loadMember(player.playerId);
        if (mrec != null) {
            // we have to make sure the gang record isn't null in case one of the servers
            // deletes the gang in the meantime
            GangRecord grec = _gangrepo.loadGang(mrec.gangId, false);
            if (grec != null) {
                info.gang = grec.getName();
                info.rank = mrec.rank;
                info.title = mrec.title;
                info.buckle = new BuckleInfo(grec.getBucklePrint());
            }
        }
    }

    /**
     * Clears any cached gang info for the named gang (both on this server and on any peers).
     */
    public void clearCachedGangInfo (Handle name)
    {
        _infoCache.remove(name);
        if (_peermgr.isRunning()) {
            _peermgr.broadcastStaleCacheData(GANG_INFO_CACHE, name);
        }
    }

    /**
     * Convenience function for persisting an increment of a gang member's leader level.  Must be
     * called on the invoker thread.
     */
    public void incLeaderLevel (GangObject gangobj, Handle handle)
        throws PersistenceException
    {
        if (handle != null) {
            GangMemberEntry leader = gangobj.members.get(handle);
            if (leader != null && leader.leaderLevel < GangHandler.LEADER_LEVEL_WAITS.length - 1) {
                // if they've waited twice as long as necessary, they get a double bump in level
                long doubleTime = leader.lastLeaderCommand +
                    2 * GangHandler.LEADER_LEVEL_WAITS[leader.leaderLevel] * GangHandler.ONE_HOUR;
                if (leader.leaderLevel > 0 && doubleTime < System.currentTimeMillis()) {
                    leader.leaderLevel =
                        Math.min(GangHandler.LEADER_LEVEL_WAITS.length-1, leader.leaderLevel+2);
                } else {
                    leader.leaderLevel++;
                }
                leader.lastLeaderCommand = System.currentTimeMillis();
                _gangrepo.updateLeaderLevel(leader.playerId, leader.leaderLevel);
            }
        }
    }

    // documentation inherited from GangProvider
    public void getGangInfo (PlayerObject caller, final Handle name,
                             final GangService.ResultListener listener) throws InvocationException
    {
        // first look in the cache
        GangInfo info = _infoCache.get(name);
        if (info != null) {
            listener.requestProcessed(info);
            return;
        }

        // see if the gang is loaded and initialized
        GangHandler handler = _names.get(name);
        if (handler != null) {
            GangObject gangobj = handler.getGangObject();
            info = new GangInfo();
            info.name = gangobj.name;
            info.founded = gangobj.founded;
            info.weightClass = gangobj.getWeightClass();
            info.notoriety = gangobj.notoriety;
            info.statement = gangobj.statement;
            info.url = gangobj.url;
            info.buckle = gangobj.getBuckleInfo();
            info.avatar = gangobj.avatar;
            info.leaders = getSortedMembers(gangobj.members, true);
            info.members = getSortedMembers(gangobj.members, false);

            _infoCache.put(info.name, info);
            listener.requestProcessed(info);
            return;
        }

        // otherwise, we must hit the database
        _invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _grec = _gangrepo.loadGang(name);
            }
            public void handleSuccess () {
                if (_grec == null) {
                    listener.requestFailed("m.no_such_gang");
                    return;
                }
                GangInfo info = new GangInfo();
                info.name = _grec.getName();
                info.founded = _grec.founded.getTime();
                info.weightClass = _grec.weightClass;
                info.notoriety = GangUtil.getNotorietyLevel(_grec.weightClass, _grec.notoriety);
                info.statement = _grec.statement;
                info.url = _grec.url;
                info.buckle = new BuckleInfo(_grec.getBucklePrint());
                info.avatar = _grec.avatar;
                info.leaders = getSortedMembers(_grec.members, true);
                info.members = getSortedMembers(_grec.members, false);

                _infoCache.put(info.name, info);
                listener.requestProcessed(info);
            }
            public String getFailureMessage () {
                return "Failed to load gang info [name=" + name + "].";
            }
            protected GangRecord _grec;
        });
    }

    // documentation inherited from GangProvider
    public void inviteMember (PlayerObject user, Handle handle, String message,
                              GangService.ConfirmListener listener) throws InvocationException
    {
        // make sure it's not the player himself
        if (user.handle.equals(handle)) {
            throw new InvocationException("e.invite_self");
        }
        // pass it off to the gang handler
        requireGangPeerProvider(user.gangId).inviteMember(
            null, user.handle, handle, message, listener);
    }

    /**
     * Returns the {@link GangPeerProvider} interface for the specified gang (which either resolves
     * to a local handler or a proxy to a handler on a peer node.
     */
    public GangPeerProvider requireGangPeerProvider (int gangId)
        throws InvocationException
    {
        return requireGang(gangId).getPeerProvider();
    }

    /**
     * Returns the {@link GangPeerProvider} interface for the specified gang (which either resolves
     * to a local handler or a proxy to a handler on a peer node).
     */
    public GangPeerProvider requireGangPeerProvider (Handle name)
        throws InvocationException
    {
        GangHandler handler = _names.get(name);
        if (handler == null || handler.getGangObject() == null) {
            log.warning("Gang not loaded or initialized", "name", name, "handler", handler);
            throw new InvocationException(INTERNAL_ERROR);
        }
        return handler.getPeerProvider();
    }

    /**
     * Retrieves the initialized gang handler for the supplied gang id, throwing an exception and
     * logging a warning if unavailable.
     */
    public GangHandler requireGang (int gangId)
        throws InvocationException
    {
        GangHandler handler = _gangs.get(gangId);
        if (handler == null || handler.getGangObject() == null) {
            log.warning("Gang not loaded or initialized", "gangId", gangId, "handler", handler);
            throw new InvocationException(INTERNAL_ERROR);
        }
        return handler;
    }

    /**
     * Loads the list of gangs asynchronously on the invoker thread.
     */
    public void loadGangs (final ResultListener<List<GangEntry>> listener)
    {
        _invoker.postUnit(new RepositoryListenerUnit<List<GangEntry>>(listener) {
            public List<GangEntry> invokePersistResult ()
                throws PersistenceException {
                return _gangrepo.loadGangs();
            }
        });
    }

    /**
     * Processes a request to form a gang.  It is assumed that the player does not already belong
     * to a gang and that the provided name is valid.
     *
     * @param listener if the request succeeds, this listener will receive the new
     * {@link GangEntry} for the gang
     */
    public void formGang (final PlayerObject user, final Handle name,
                          final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // store the user's avatar
        Look look = user.getLook(Look.Pose.WANTED_POSTER);
        final AvatarInfo avatar = (look == null) ? null : look.getAvatar(user);

        // create the buckle bits in the omgr thread
        final BucklePart[] parts = _alogic.createDefaultBuckle();

        // start up the action
        _invoker.post(new FinancialAction(user, FORM_GANG_SCRIP_COST, FORM_GANG_COIN_COST) {
            protected String getCoinDescrip () {
                return MessageBundle.tcompose("m.gang_creation", name);
            }

            protected String persistentAction ()
                throws PersistenceException {
                if (!_gangrepo.insertGang(_grec)) {
                    // we check this in the database, even though the HideoutManager checked it
                    // against the local list, because another server may have created a gang
                    // with the same name
                    return "m.duplicate_gang_name";
                }
                _gangrepo.insertMember(
                    _mrec = new GangMemberRecord(user.playerId, _grec.gangId, LEADER_RANK));
                _gangrepo.insertHistoryEntry(
                    _grec.gangId, MessageBundle.tcompose("m.founded_entry", user.handle));
                _grec.members.add(new GangMemberEntry(
                    user.handle, user.playerId, LEADER_RANK, 0, 0, _mrec.lastLeaderCommand,
                    _mrec.joined, 0, 0, 0, 0, _mrec.joined));
                _grec.avatar = avatar;

                // set the buckle parts' owner ids before inserting them, then note their
                // item ids for the buckle field
                int[] bids = new int[parts.length];
                for (int ii = 0; ii < parts.length; ii++) {
                    BucklePart part = parts[ii];
                    part.setOwnerId(_grec.gangId);
                    _itemrepo.insertItem(part);
                    _grec.inventory.add(part);
                    bids[ii] = part.getItemId();
                }
                BuckleInfo buckle = GangUtil.getBuckleInfo(parts);
                _gangrepo.updateBuckle(_grec.gangId, bids, buckle.print);
                _grec.setBuckle(bids, buckle.print);
                return null;
            }
            protected void rollbackPersistentAction ()
                throws PersistenceException {
                // deleting the gang also deletes its history
                _gangrepo.deleteGang(_grec.gangId);
                _gangrepo.deleteMember(user.playerId);
                for (Item item : _grec.inventory) {
                    _itemrepo.deleteItem(item, "rollback");
                }
            }

            protected void actionCompleted () {
                log.info("Formed new gang", "who", user.who(), "name", name,
                         "gangId", _grec.gangId);
                BangServer.hideoutmgr.activateGang(name);
                if (user.isActive()) {
                    _gangs.put(_grec.gangId, GangHandler.newGangHandler(
                        _injector, _grec, user, listener));
                }
                BangServer.generalLog("joined_gang " + user.playerId + " g:" + _grec.gangId);
                super.actionCompleted();
            }
            protected void actionFailed (String cause) {
                listener.requestFailed(cause);
            }

            protected String getPurchaseType () {
                return "gang";
            }
            protected String getGoodType () {
                return "Gang";
            }

            protected GangRecord _grec = new GangRecord(name);
            protected GangMemberRecord _mrec;
        });
    }

    /**
     * Sends a gang invite to the specified player if he is online (on any server).
     */
    public void sendGangInvite (
        Handle invitee, Handle inviter, int gangId, Handle name, String message)
    {
        PlayerObject user = BangServer.locator.lookupPlayer(invitee);
        if (user != null) {
            sendGangInviteLocal(user, inviter, gangId, name, message);
        } else if (_peermgr.isRunning()) {
            _peermgr.forwardGangInvite(invitee, inviter, gangId, name, message);
        }
    }

    /**
     * Sends an invitation to join a gang (on this server only).
     */
    public void sendGangInviteLocal (
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
     * Erodes the notoriety of all gangs, then syncronizes the notoriety levels.
     */
    public void erodeNotoriety ()
    {
        _invoker.postUnit(new RepositoryUnit("erodeNotoriety") {
            public void invokePersist ()
                throws PersistenceException {
                _gangrepo.erodeNotoriety();
            }
            public void handleSuccess () {
                syncNotoriety();
                if (_peermgr.isRunning()) {
                    _peermgr.broadcastStaleCacheData(GANG_NOTORIETY_CACHE, null);
                }
            }
            public void handleFailure (Exception cause) {
                log.warning("Failed to erode gang notorieties", "error", cause);
            }
        });
    }

    /**
     * Syncronize the in memory notoriety with the persisted value.
     */
    public void syncNotoriety ()
    {
        _invoker.postUnit(new RepositoryUnit("syncNotoriety") {
            public void invokePersist ()
                throws PersistenceException {
                _notMap = _gangrepo.loadGangsNotoriety(_gangs.keys());
            }
            public void handleSuccess () {
                for (IntIntMap.IntIntEntry entry : _notMap.entrySet()) {
                    GangHandler handler = _gangs.get(entry.getIntKey());
                    // make sure this is a local handler
                    if (handler != null && handler == handler.getPeerProvider()) {
                        handler.setNotoriety(entry.getIntValue());
                    }
                }
            }
            public void handleFailure (Exception cause) {
                log.warning("Failed to sync gang notorieties.", cause);
            }

            protected IntIntMap _notMap;
        });
    }

    /**
     * Processes the response to a gang invitation.
     */
    protected void handleInviteResponse (
        final PlayerObject user, final Handle inviter, final int gangId, final Handle name,
        final boolean accept, final InvocationService.ConfirmListener listener)
    {
        // make sure the gang's resolved, then pass it off to the handler
        resolveGang(gangId, new ResultListener<GangObject>() {
            public void requestCompleted (GangObject result) {
                try {
                    requireGangPeerProvider(gangId).handleInviteResponse(
                        null, user.handle, user.playerId, inviter, accept, listener);
                } catch (InvocationException e) {
                    listener.requestFailed(e.getMessage());
                }
            }
            public void requestFailed (Exception cause) {
                listener.requestFailed(INTERNAL_ERROR);
            }
        });
    }

    /**
     * Maps a gang by its name (once we know it).
     */
    protected void mapGang (Handle name, GangHandler gang)
    {
        _names.put(name, gang);
    }

    /**
     * Removes a gang from our mapping after it has been shut down.
     */
    protected void unmapGang (int gangId, Handle name)
    {
        _gangs.remove(gangId);
        if (name != null) {
            _names.remove(name);
        }
    }

    /**
     * Converts an actual rank to a rank appropriate for display on a poster.
     */
    protected static byte getPosterRank (byte rank)
    {
        return (rank == RECRUITER_RANK) ? MEMBER_RANK : rank;
    }

    /**
     * Creates a member info array containing the leaders or normal members extracted from the
     * given collection, sorted by the criteria defined in {@link GangUtil} (without sorting by
     * online status).
     */
    protected static GangInfo.Member[] getSortedMembers (
        Iterable<GangMemberEntry> members, boolean leaders)
    {
        List<GangMemberEntry> entries = GangUtil.getSortedMembers(members, false, leaders);
        GangInfo.Member[] info = new GangInfo.Member[entries.size()];
        for (int ii = 0; ii < info.length; ii++) {
            GangMemberEntry entry = entries.get(ii);
            info[ii] = new GangInfo.Member(entry.handle, entry.isActive());
        }
        return info;
    }

    /** Maps gang ids to currently loaded gang objects. */
    protected HashIntMap<GangHandler> _gangs = new HashIntMap<GangHandler>();

    /** Maps gang names to initialized gangs. */
    protected HashMap<Handle, GangHandler> _names = new HashMap<Handle, GangHandler>();

    /** A light-weight cache of soft {@link GangInfo} references. */
    protected SoftCache<Handle, GangInfo> _infoCache = new SoftCache<Handle, GangInfo>();

    // dependencies
    @Inject protected Injector _injector;
    @Inject protected BangInvoker _invoker;
    @Inject protected AvatarLogic _alogic;
    @Inject protected BangPeerManager _peermgr;
    @Inject protected GangRepository _gangrepo;
    @Inject protected ItemRepository _itemrepo;

    /** The name of our gang info cache. */
    protected static final String GANG_INFO_CACHE = "gangInfoCache";

    /** The name of our gang notoriety cache. */
    protected static final String GANG_NOTORIETY_CACHE = "gangNotorietyCache";

    /** The interval with which we erode gang notoriety levels. */
    protected static final long ERODE_NOTORIETY_INTERVAL = 24 * 60 * 60 * 1000L;
}
