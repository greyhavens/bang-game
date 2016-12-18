//
// $Id$

package com.threerings.bang.server;

import java.util.Date;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Lifecycle;
import com.samskivert.util.ObserverList;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;
import com.threerings.util.Name;
import com.threerings.util.StreamableTuple;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerNode;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsSession;

import com.threerings.crowd.peer.server.CrowdPeerManager;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.data.BangPeerMarshaller;

import com.threerings.bang.gang.data.GangObject;

import com.threerings.bang.client.BangPeerService;
import com.threerings.bang.data.BangClientInfo;
import com.threerings.bang.data.BangNodeObject;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;

/**
 * Extends the standard peer services and handles some Bang specific business
 * like pardner presence reporting.
 */
@Singleton
public class BangPeerManager extends CrowdPeerManager
    implements BangPeerProvider
{
    /**
     * Used by entities that wish to know when a player logs onto or off of this or one of our peer
     * servers.
     */
    public static interface RemotePlayerObserver
    {
        /**
         * Called when a player logs on to one of our peer servers.
         */
        public void remotePlayerLoggedOn (int townIndex, BangClientInfo info);

        /**
         * Called when a player logs off of one of our peer servers.
         */
        public void remotePlayerLoggedOff (int townIndex, BangClientInfo info);

        /**
         * Called when a remote player changes his handle.
         */
        public void remotePlayerChangedHandle (int townIndex, Handle oldHandle, Handle newHandle);
    }

    /**
     * Creates an uninitialized peer manager.
     */
    @Inject public BangPeerManager (Lifecycle cycle)
    {
        super(cycle);
    }

    /**
     * Returns true if we're running in a peer configuration, false otherwise.
     */
    public boolean isRunning ()
    {
        return (_nodeName != null);
    }

    /**
     * Registers a remote player observer.
     */
    public void addPlayerObserver (RemotePlayerObserver observer)
    {
        _remobs.add(observer);
    }

    /**
     * Removes a remote player observer registration.
     */
    public void removePlayerObserver (RemotePlayerObserver observer)
    {
        _remobs.remove(observer);
    }

    /**
     * Returns information on the specified player if they are currently logged onto one of our
     * peer servers.
     *
     * @return the player's client info and the town index of the node to which they are connected
     * or null if the player is connected to no peer node.
     */
    public Tuple<BangClientInfo,Integer> locateRemotePlayer (Handle handle)
    {
        for (PeerNode peer : _peers.values()) {
            if (peer.nodeobj == null) { // skip uninitialized peers
                continue;
            }
            BangClientInfo info = (BangClientInfo)peer.nodeobj.clients.get(handle);
            if (info != null) {
                return new Tuple<BangClientInfo,Integer>(info, ((BangPeerNode)peer).townIndex);
            }
        }
        return null;
    }

    /**
     * Requests to deliver the a pardner invite to a player if he's logged into one of our peer
     * servers.
     */
    public void forwardPardnerInvite (Handle invitee, Handle inviter, String message)
    {
        PeerNode peer = getPlayerPeer(invitee);
        if (peer != null) {
            ((BangNodeObject)peer.nodeobj).bangPeerService.deliverPardnerInvite(
                invitee, inviter, message);
        }
    }

    // from interface BangPeerProvider
    public void deliverPardnerInvite (
        ClientObject caller, Handle invitee, Handle inviter, String message)
    {
        PlayerObject user = BangServer.locator.lookupPlayer(invitee);
        if (user != null) {
            BangServer.playmgr.sendPardnerInviteLocal(user, inviter, message, new Date());
        }
    }

    /**
     * Requests to deliver the specified invite response if the inviter is logged into one of our
     * peer servers.
     */
    public void forwardPardnerInviteResponse (
        Handle inviter, Handle invitee, boolean accept, boolean full)
    {
        for (PeerNode peer : _peers.values()) {
            if (peer.nodeobj == null) {
                continue;
            }
            if (peer.nodeobj.clients.containsKey(inviter)) {
                ((BangNodeObject)peer.nodeobj).bangPeerService.deliverPardnerInviteResponse(
                    inviter, invitee, accept, full);
                return;
            }
        }
    }

    // from interface BangPeerProvider
    public void deliverPardnerInviteResponse (
        ClientObject caller, Handle inviter, Handle invitee, boolean accept, boolean full)
    {
        PlayerObject user = BangServer.locator.lookupPlayer(inviter);
        if (user != null) {
            BangServer.playmgr.respondToPardnerInviteLocal(user, invitee, accept, full);
        }
    }

    /**
     * Requests to remove the specified pardner if the removee is logged into one of our peer
     * servers.
     */
    public void forwardPardnerRemoval (Handle removee, Handle remover)
    {
        for (PeerNode peer : _peers.values()) {
            if (peer.nodeobj == null) {
                continue;
            }
            if (peer.nodeobj.clients.containsKey(removee)) {
                ((BangNodeObject)peer.nodeobj).bangPeerService.deliverPardnerRemoval(
                    removee, remover);
                return;
            }
        }
    }

    // from interface BangPeerProvider
    public void deliverPardnerRemoval (ClientObject caller, Handle removee, Handle remover)
    {
        PlayerObject user = BangServer.locator.lookupPlayer(removee);
        if (user != null) {
            BangServer.playmgr.removePardnerLocal(user, remover);
        }
    }

    /**
     * Requests to deliver a gang invite to a player if he's logged into one of our peer servers.
     */
    public void forwardGangInvite (
        Handle invitee, Handle inviter, int gangId, Handle name, String message)
    {
        PeerNode peer = getPlayerPeer(invitee);
        if (peer != null) {
            ((BangNodeObject)peer.nodeobj).bangPeerService.deliverGangInvite(
                invitee, inviter, gangId, name, message);
        }
    }

    // from interface BangPeerProvider
    public void deliverGangInvite (
        ClientObject caller, Handle invitee, Handle inviter, int gangId, Handle name,
        String message)
    {
        PlayerObject user = BangServer.locator.lookupPlayer(invitee);
        if (user != null) {
            BangServer.gangmgr.sendGangInviteLocal(user, inviter, gangId, name, message);
        }
    }

    /**
     * Requests to deliver the specified item to its owner if he's logged into one of our peer
     * servers.
     *
     * @param source a qualified translatable string describing the source of the item
     */
    public void forwardItem (Item item, String source)
    {
        for (PeerNode peer : _peers.values()) {
            if (peer.nodeobj == null) {
                continue;
            }
            if (((BangPeerNode)peer).players.containsKey(item.getOwnerId())) {
                ((BangNodeObject)peer.nodeobj).bangPeerService.deliverItem(item, source);
                return;
            }
        }
    }

    // from interface BangPeerProvider
    public void deliverItem (ClientObject caller, Item item, String source)
    {
        PlayerObject user = BangServer.locator.lookupPlayer(item.getOwnerId());
        if (user != null) {
            BangServer.playmgr.deliverItemLocal(user, item, source);
        }
    }

    /**
     * Subscribes to the specified gang on the given node.
     */
    public void subscribeToGang (
        final String nodeName, int gangId, final ResultListener<GangObject> listener)
    {
        PeerNode peer = _peers.get(nodeName);
        if (peer == null) {
            String msg = "Unknown node for gang subscription [name=" + nodeName + "].";
            listener.requestFailed(new Exception(msg));
            return;
        }
        ((BangNodeObject)peer.nodeobj).bangPeerService.getGangOid(gangId,
            new BangPeerService.ResultListener() {
                public void requestProcessed (Object result) {
                    continueSubscribingToGang(nodeName, (Integer)result, listener);
                }
                public void requestFailed (String cause) {
                    listener.requestFailed(new InvocationException(cause));
                }
            });
    }

    // from interface BangPeerProvider
    public void getGangOid (
        ClientObject caller, int gangId, final InvocationService.ResultListener listener)
    {
        BangServer.gangmgr.resolveGang(gangId,
            new ResultAdapter<GangObject>(listener) {
                public void requestCompleted (GangObject result) {
                    listener.requestProcessed(result.getOid());
                }
            });
    }

    @Override // from CrowdPeerManager
    public void shutdown ()
    {
        super.shutdown();

        // clear out our invocation service
        if (_nodeobj != null) {
            BangServer.invmgr.clearDispatcher(((BangNodeObject)_nodeobj).bangPeerService);
        }
    }

    @Override // from PeerManager
    protected Class<? extends PeerNode> getPeerNodeClass ()
    {
        return BangPeerNode.class;
    }

    @Override // from CrowdPeerManager
    protected NodeObject createNodeObject ()
    {
        return new BangNodeObject();
    }

    @Override // from CrowdPeerManager
    protected ClientInfo createClientInfo ()
    {
        return new BangClientInfo();
    }

    @Override // from CrowdPeerManager
    protected void initClientInfo (PresentsSession client, ClientInfo info)
    {
        super.initClientInfo(client, info);
        BangClientInfo binfo = (BangClientInfo)info;
        PlayerObject player = (PlayerObject)client.getClientObject();
        binfo.playerId = player.playerId;

        // grab a snapshot of this player's avatar which is how they'll look to
        // pardners on other servers
        Look look = player.getLook(Look.Pose.DEFAULT);
        binfo.avatar = (look == null) ? null : look.getAvatar(player);
    }

    @Override // from CrowdPeerManager
    protected void didInit ()
    {
        super.didInit();

        // stuff our town information into our node object
        final BangNodeObject bnodeobj = (BangNodeObject)_nodeobj;
        bnodeobj.setTownId(ServerConfig.townId);
        bnodeobj.setBangPeerService(
            BangServer.invmgr.registerProvider(this, BangPeerMarshaller.class));

        // subscribe to server for handle change notifications
        BangServer.locator.addPlayerObserver(new PlayerLocator.PlayerObserver() {
            public void playerLoggedOn (PlayerObject user) {
                // no-op
            }
            public void playerLoggedOff (PlayerObject user) {
                // no-op
            }
            public void playerChangedHandle (PlayerObject user, Handle oldHandle) {
                bnodeobj.startTransaction();
                try {
                    // "log off" the old handle
                    BangClientInfo info = (BangClientInfo)bnodeobj.clients.get(oldHandle);
                    if (info != null) {
                        bnodeobj.removeFromClients(oldHandle);
                    }

                    // broadcast the change on our node object
                    bnodeobj.setChangedHandle(
                        new StreamableTuple<Handle, Handle>(oldHandle, user.handle));

                    // "log on" with the new
                    if (info != null) {
                        info.visibleName = user.handle;
                        bnodeobj.addToClients(info);
                    }
                } finally {
                    bnodeobj.commitTransaction();
                }
            }
        });
    }

    @Override // from CrowdPeerManager
    protected Name authFromViz (Name vizname)
    {
        return _authFromViz.get(vizname);
    }

    /**
     * Called when a player logs onto one of our peer servers.
     */
    protected void remotePlayerLoggedOn (final int townIndex, final BangClientInfo info)
    {
        // maintain a mapping from vizname to authname
        _authFromViz.put(info.visibleName, info.username);

        // notify our remote player observers
        _remobs.apply(new ObserverList.ObserverOp<RemotePlayerObserver>() {
            public boolean apply (RemotePlayerObserver observer) {
                observer.remotePlayerLoggedOn(townIndex, info);
                return true;
            }
        });
    }

    /**
     * Called when a player logs off of one of our peer servers.
     */
    protected void remotePlayerLoggedOff (final int townIndex, final BangClientInfo info)
    {
        // clear our mapping from vizname to authname
        _authFromViz.remove(info.visibleName);

        // notify our remote player observers
        _remobs.apply(new ObserverList.ObserverOp<RemotePlayerObserver>() {
            public boolean apply (RemotePlayerObserver observer) {
                observer.remotePlayerLoggedOff(townIndex, info);
                return true;
            }
        });
    }

    /**
     * Called when a player changes his handle on one of our peer servers.
     */
    protected void remotePlayerChangedHandle (
        final int townIndex, final Handle oldHandle, final Handle newHandle)
    {
        // notify our remote player observers
        _remobs.apply(new ObserverList.ObserverOp<RemotePlayerObserver>() {
            public boolean apply (RemotePlayerObserver observer) {
                observer.remotePlayerChangedHandle(townIndex, oldHandle, newHandle);
                return true;
            }
        });
    }

    /**
     * Returns the peer node on which the identified user is logged in, if any.
     */
    protected PeerNode getPlayerPeer (Handle handle)
    {
        for (PeerNode peer : _peers.values()) {
            if (peer.nodeobj != null && peer.nodeobj.clients.containsKey(handle)) {
                return peer;
            }
        }
        return null;
    }

    /**
     * Continues the process of subscribing to a gang once we know its oid on the peer.
     */
    protected void continueSubscribingToGang (
        String nodeName, final int remoteOid, final ResultListener<GangObject> listener)
    {
        proxyRemoteObject(nodeName, remoteOid,
            new ResultListener<Integer>() {
                public void requestCompleted (Integer result) {
                    GangObject gangobj = (GangObject)BangServer.omgr.getObject(result);
                    gangobj.remoteOid = remoteOid;
                    listener.requestCompleted(gangobj);
                }
                public void requestFailed (Exception cause) {
                    listener.requestFailed(cause);
                }
            });
    }

    protected Map<Name, Name> _authFromViz = Maps.newHashMap();
    protected ObserverList<RemotePlayerObserver> _remobs = ObserverList.newFastUnsafe();
}
