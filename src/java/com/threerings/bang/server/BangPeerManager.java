//
// $Id$

package com.threerings.bang.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Invoker;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.persist.NodeRecord;
import com.threerings.presents.server.PresentsClient;

import com.threerings.crowd.peer.server.CrowdPeerManager;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.data.BangClientInfo;
import com.threerings.bang.data.PlayerObject;

/**
 * Extends the standard peer services and handles some Bang specific business
 * like pardner presence reporting.
 */
public class BangPeerManager extends CrowdPeerManager
{
    public BangPeerManager (ConnectionProvider conprov, Invoker invoker)
        throws PersistenceException
    {
        super(conprov, invoker);
    }

    @Override // documentation inherited
    protected PeerNode createPeerNode (NodeRecord record)
    {
        return new BangPeerNode(record);
    }

    @Override // documentation inherited
    protected ClientInfo createClientInfo ()
    {
        return new BangClientInfo();
    }

    @Override // documentation inherited
    protected void initClientInfo (PresentsClient client, ClientInfo info)
    {
        super.initClientInfo(client, info);

        // grab a snapshot of this player's avatar which is how they'll look to
        // pardners on other servers
        PlayerObject player = (PlayerObject)client.getClientObject();
        Look look = player.getLook(Look.Pose.DEFAULT);
        if (look != null) {
            ((BangClientInfo)info).avatar = look.getAvatar(player);
        }
    }

    /**
     * Called when a player logs onto one of our peer servers.
     */
    protected void playerLoggedOn (BangClientInfo info)
    {
        // TODO: sort out buddy bits
    }

    /**
     * Called when a player logs off of one of our peer servers.
     */
    protected void playerLoggedOff (BangClientInfo info)
    {
        // TODO: sort out buddy bits
    }

    protected class BangPeerNode extends PeerNode
        implements SetListener
    {
        public BangPeerNode (NodeRecord record) {
            super(record);
        }

        @Override // documentation inherited
        public void objectAvailable (NodeObject object) {
            super.objectAvailable(object);
        }

        public void entryAdded (EntryAddedEvent event) {
            if (event.getName().equals(NodeObject.CLIENTS)) {
                playerLoggedOn((BangClientInfo)event.getEntry());
            }
        }

        public void entryUpdated (EntryUpdatedEvent event) {
        }

        public void entryRemoved (EntryRemovedEvent event) {
            if (event.getName().equals(NodeObject.CLIENTS)) {
                playerLoggedOff((BangClientInfo)event.getOldEntry());
            }
        }
    }
}
