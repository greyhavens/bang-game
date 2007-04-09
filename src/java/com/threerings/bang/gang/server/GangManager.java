//
// $Id$

package com.threerings.bang.gang.server;

import java.lang.ref.SoftReference;

import java.util.ArrayList;
import java.util.HashMap;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.RepositoryListenerUnit;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SoftCache;

import com.threerings.io.Streamable;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.presents.peer.server.PeerManager;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.PosterInfo;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;
import com.threerings.bang.server.persist.PlayerRecord;

import com.threerings.bang.avatar.data.Look;

import com.threerings.bang.gang.client.GangService;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangEntry;
import com.threerings.bang.gang.data.GangInfo;
import com.threerings.bang.gang.data.GangInvite;
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
public class GangManager
    implements GangProvider, GangCodes
{
    /**
     * Initializes the gang manager and registers its invocation service.
     */
    public void init (ConnectionProvider conprov)
        throws PersistenceException
    {
        _gangrepo = new GangRepository(conprov);

        // register ourselves as the provider of the (bootstrap) GangService
        BangServer.invmgr.registerDispatcher(new GangDispatcher(this), GLOBAL_GROUP);

        // listen for gang info cache updates
        if (BangServer.peermgr != null) {
            BangServer.peermgr.addStaleCacheObserver(GANG_INFO_CACHE,
                new PeerManager.StaleCacheObserver() {
                    public void changedCacheData (Streamable data) {
                        _infoCache.remove((Handle)data);
                    }
                });
        }
    }

    /**
     * Resolves the specified gang, either loading it from the database (if it has not been
     * loaded on any other servers), or subscribing to it through another server.
     */
    public void resolveGang (int gangId, ResultListener<GangObject> listener)
    {
        GangHandler gang = _gangs.get(gangId);
        if (gang == null) {
            _gangs.put(gangId, gang = new GangHandler(gangId));
        }
        gang.getGangObject(listener);
    }

    /**
     * Registers a just-logged-on player with their Gang, if appropriate, dispatches any pending
     * Gang invitations otherwise. Called during logon after a player has resolved all of their
     * persistent data.
     */
    public void initPlayer (
        PlayerObject player, GangMemberRecord mrec, ArrayList<GangInviteRecord> invites)
    {
        BangServer.requireDObjThread(); // safety first

        // if they're a gang member, wire them up; otherwise, dispatch any pending gang invitations
        if (mrec != null) {
            try {
                requireGang(mrec.gangId).initPlayer(player);
            } catch (InvocationException e) {
                log.warning("Gang not loaded to init player [gangId=" + mrec.gangId + ", player=" +
                    player.who() + "].");
            }
        } else if (invites != null) {
            for (GangInviteRecord record : invites) {
                sendGangInviteLocal(player, record.inviter, record.gangId, record.name,
                    record.message);
            }
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
            info.rank = getPosterRank(player.gangRank);
            info.buckle = gangobj.getBuckleInfo();
        } catch (InvocationException e) {
            log.warning("Gang not loaded to populate poster [gangId=" + player.gangId +
                ", player=" + player.who() + "].");
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
                info.rank = getPosterRank(mrec.rank);
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
        if (BangServer.peermgr != null) {
            BangServer.peermgr.broadcastStaleCacheData(GANG_INFO_CACHE, name);
        }
    }

    // documentation inherited from GangProvider
    public void getGangInfo (
        ClientObject caller, final Handle name, final GangService.ResultListener listener)
        throws InvocationException
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
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
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
    public void inviteMember (
        ClientObject caller, final Handle handle, final String message,
        final GangService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it's not the player himself
        PlayerObject user = (PlayerObject)caller;
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
     * Retrieves the initialized gang handler for the supplied gang id, throwing an exception and
     * logging a warning if unavailable.
     */
    public GangHandler requireGang (int gangId)
        throws InvocationException
    {
        GangHandler handler = _gangs.get(gangId);
        if (handler == null || handler.getGangObject() == null) {
            log.warning("Gang not loaded or initialized [gangId=" + gangId + ", handler=" +
                handler + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        return handler;
    }

    /**
     * Loads the list of gangs asynchronously on the invoker thread.
     */
    public void loadGangs (final ResultListener<ArrayList<GangEntry>> listener)
    {
        BangServer.invoker.postUnit(new RepositoryListenerUnit<ArrayList<GangEntry>>(listener) {
            public ArrayList<GangEntry> invokePersistResult ()
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
        final BucklePart[] parts = BangServer.alogic.createDefaultBuckle();

        // start up the action
        new FinancialAction(user, FORM_GANG_SCRIP_COST, FORM_GANG_COIN_COST) {
            protected int getCoinType () {
                return CoinTransaction.GANG_CREATION;
            }
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
                    user.handle, user.playerId, LEADER_RANK, _mrec.joined, 0, _mrec.joined));
                _grec.avatar = avatar;

                // set the buckle parts' owner ids before inserting them, then note their
                // item ids for the buckle field
                int[] bids = new int[parts.length];
                for (int ii = 0; ii < parts.length; ii++) {
                    BucklePart part = parts[ii];
                    part.setOwnerId(_grec.gangId);
                    BangServer.itemrepo.insertItem(part);
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
                    BangServer.itemrepo.deleteItem(item, "rollback");
                }
            }

            protected void actionCompleted () {
                log.info("Formed new gang [who=" + user.who() + ", name=" + name +
                         ", gangId=" + _grec.gangId + "].");
                BangServer.hideoutmgr.activateGang(name);
                if (user.isActive()) {
                    _gangs.put(_grec.gangId, new GangHandler(_grec, user, listener));
                }
            }
            protected void actionFailed (String cause) {
                listener.requestFailed(cause);
            }

            protected GangRecord _grec = new GangRecord(name);
            protected GangMemberRecord _mrec;
        }.start();
    }

    /**
     * Sends a gang invite to the specified player if he is online (on any server).
     */
    public void sendGangInvite (
        Handle invitee, Handle inviter, int gangId, Handle name, String message)
    {
        PlayerObject user = BangServer.lookupPlayer(invitee);
        if (user != null) {
            sendGangInviteLocal(user, inviter, gangId, name, message);
        } else if (BangServer.peermgr != null) {
            BangServer.peermgr.forwardGangInvite(invitee, inviter, gangId, name, message);
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
     * given collection, sorted by the criteria defined in {@link GangUtil}.
     */
    protected static GangInfo.Member[] getSortedMembers (
        Iterable<GangMemberEntry> members, boolean leaders)
    {
        ArrayList<GangMemberEntry> entries = GangUtil.getSortedMembers(members, leaders);
        GangInfo.Member[] info = new GangInfo.Member[entries.size()];
        for (int ii = 0; ii < info.length; ii++) {
            GangMemberEntry entry = entries.get(ii);
            info[ii] = new GangInfo.Member(entry.handle, entry.isActive());
        }
        return info;
    }

    /** The persistent store for gang data. */
    protected GangRepository _gangrepo;

    /** Maps gang ids to currently loaded gang objects. */
    protected HashIntMap<GangHandler> _gangs = new HashIntMap<GangHandler>();

    /** Maps gang names to initialized gangs. */
    protected HashMap<Handle, GangHandler> _names = new HashMap<Handle, GangHandler>();

    /** A light-weight cache of soft {@link GangInfo} references. */
    protected SoftCache<Handle, GangInfo> _infoCache = new SoftCache<Handle, GangInfo>();

    /** The name of our gang info cache. */
    protected static final String GANG_INFO_CACHE = "gangInfoCache";
}
