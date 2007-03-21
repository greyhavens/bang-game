//
// $Id$

package com.threerings.bang.server;

import java.util.Date;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObserverList;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.persist.NodeRecord;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsClient;

import com.threerings.crowd.peer.server.CrowdPeerManager;

import com.threerings.bang.avatar.data.Look;

import com.threerings.bang.gang.data.GangObject;

import com.threerings.bang.client.BangPeerService;
import com.threerings.bang.data.BangClientInfo;
import com.threerings.bang.data.BangNodeObject;
import com.threerings.bang.data.BangPeerMarshaller;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Notification;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * Extends the standard peer services and handles some Bang specific business
 * like pardner presence reporting.
 */
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
         * Called when a player logs on to this or one of our peer servers.
         */
        public void remotePlayerLoggedOn (int townIndex, BangClientInfo info);

        /**
         * Called when a player logs off of this or one of our peer servers.
         */
        public void remotePlayerLoggedOff (int townIndex, BangClientInfo info);
    }

    public BangPeerManager (ConnectionProvider conprov, Invoker invoker)
        throws PersistenceException
    {
        super(conprov, invoker);
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
                peer.getClient(), invitee, inviter, message);
        }
    }

    // from interface BangPeerProvider
    public void deliverPardnerInvite (
        ClientObject caller, Handle invitee, Handle inviter, String message)
    {
        PlayerObject user = BangServer.lookupPlayer(invitee);
        if (user != null) {
            BangServer.playmgr.sendPardnerInviteLocal(user, inviter, message, new Date());
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
                peer.getClient(), invitee, inviter, gangId, name, message);
        }
    }

    // from interface BangPeerProvider
    public void deliverGangInvite (
        ClientObject caller, Handle invitee, Handle inviter, int gangId, Handle name,
        String message)
    {
        PlayerObject user = BangServer.lookupPlayer(invitee);
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
                ((BangNodeObject)peer.nodeobj).bangPeerService.deliverItem(
                    peer.getClient(), item, source);
                return;
            }
        }
    }

    // from interface BangPeerProvider
    public void deliverItem (ClientObject caller, Item item, String source)
    {
        PlayerObject user = BangServer.lookupPlayer(item.getOwnerId());
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
        ((BangNodeObject)peer.nodeobj).bangPeerService.getGangOid(peer.getClient(), gangId,
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
    protected PeerNode createPeerNode (NodeRecord record)
    {
        return new BangPeerNode(record);
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
    protected void initClientInfo (PresentsClient client, ClientInfo info)
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
        BangNodeObject bnodeobj = (BangNodeObject)_nodeobj;
        bnodeobj.setTownId(ServerConfig.townId);
        bnodeobj.setBangPeerService(
            (BangPeerMarshaller)BangServer.invmgr.registerDispatcher(new BangPeerDispatcher(this)));
    }

    /**
     * Called when a player logs onto one of our peer servers.
     */
    protected void remotePlayerLoggedOn (final int townIndex, final BangClientInfo info)
    {
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
        // notify our remote player observers
        _remobs.apply(new ObserverList.ObserverOp<RemotePlayerObserver>() {
            public boolean apply (RemotePlayerObserver observer) {
                observer.remotePlayerLoggedOff(townIndex, info);
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

    protected class BangPeerNode extends PeerNode
        implements SetListener
    {
        protected int townIndex;
        protected HashIntMap<BangClientInfo> players = new HashIntMap<BangClientInfo>();

        public BangPeerNode (NodeRecord record) {
            super(record);
        }

        @Override // from PeerNode
        public void objectAvailable (NodeObject object) {
            super.objectAvailable(object);

            // look up this node's town index once and store it
            townIndex = BangUtil.getTownIndex(((BangNodeObject)object).townId);
            log.info("Got peer object " + townIndex);

            // map and issue a remotePlayerLoggedOn for all logged on players
            for (ClientInfo info : object.clients) {
                BangClientInfo binfo = (BangClientInfo)info;
                players.put(binfo.playerId, binfo);
                remotePlayerLoggedOn(townIndex, binfo);
            }
        }

        @Override // from PeerNode
        public void attributeChanged (AttributeChangedEvent event)
        {
            super.attributeChanged(event);

            // pass gang directory updates to the HideoutManager
            String name = event.getName();
            if (name.equals(BangNodeObject.ACTIVATED_GANG)) {
                BangServer.hideoutmgr.activateGangLocal((Handle)event.getValue());
            } else if (name.equals(BangNodeObject.REMOVED_GANG)) {
                BangServer.hideoutmgr.removeGangLocal((Handle)event.getValue());
            }
        }

        public void entryAdded (EntryAddedEvent event) {
            // log.info("Remote entry added " + event);
            if (event.getName().equals(NodeObject.CLIENTS)) {
                BangClientInfo info = (BangClientInfo)event.getEntry();
                players.put(info.playerId, info);
                remotePlayerLoggedOn(townIndex, info);
            }
        }

        public void entryUpdated (EntryUpdatedEvent event) {
        }

        public void entryRemoved (EntryRemovedEvent event) {
            // log.info("Remote entry removed " + event);
            if (event.getName().equals(NodeObject.CLIENTS)) {
                BangClientInfo info = (BangClientInfo)event.getOldEntry();
                players.remove(info.playerId);
                remotePlayerLoggedOff(townIndex, info);
            }
        }
    }

    protected ObserverList<RemotePlayerObserver> _remobs = new ObserverList<RemotePlayerObserver>(
        ObserverList.FAST_UNSAFE_NOTIFY);
}
