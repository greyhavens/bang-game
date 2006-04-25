//
// $Id$

package com.threerings.bang.server;

import java.util.Iterator;
import java.util.logging.Level;

import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;

import com.threerings.crowd.server.CrowdClient;
import com.threerings.presents.net.BootstrapData;

import com.threerings.bang.avatar.data.Look;

import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.BangTokenRing;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;

import static com.threerings.bang.Log.log;

/**
 * Extends {@link CrowdClient} and customizes it for Bang! Howdy.
 */
public class BangClient extends CrowdClient
{
    @Override // documentation inherited
    public void shutdown ()
    {
        super.shutdown();

        // this session is over, make a note of it
        recordEndedSession();
    }

    @Override // documentation inherited
    protected void sessionWillStart ()
    {
        super.sessionWillStart();
        PlayerObject user = (PlayerObject)_clobj;

        // register the player with their handle if they have one
        if (user.handle != null) {
            BangServer.registerPlayer(user);
        }

        // if we have auth data in the form of a token ring, use it
        if (_authdata instanceof BangTokenRing) {
            // we can set things directly here rather than use the setter
            // methods because the user object is not yet out in the wild
            user.tokens = (BangTokenRing)_authdata;
        } else {
            log.warning("Missing or bogus authdata [who=" + _username +
                        ", adata=" + _authdata + "].");
            // give them zero privileges
            user.tokens = new BangTokenRing();
        }

        // configure the player in the town for this server
        user.townId = ServerConfig.getTownId();

        // make a note of their current avatar poses for later comparison and
        // potential updating
        _startPoses = (String[])user.poses.clone();
    }

    @Override // documentation inherited
    protected BootstrapData createBootstrapData ()
    {
        return new BangBootstrapData();
    }

    @Override // documentation inherited
    protected void populateBootstrapData (BootstrapData data)
    {
        super.populateBootstrapData(data);

        // fill in the oids of important places
        BangBootstrapData bbd = (BangBootstrapData)data;
        if (((PlayerObject)_clobj).tokens.isAdmin()) {
            bbd.statusOid = BangServer.statobj.getOid();
        }
        bbd.townOid = BangServer.townobj.getOid();
        bbd.saloonOid = BangServer.saloonmgr.getPlaceObject().getOid();
        bbd.storeOid = BangServer.storemgr.getPlaceObject().getOid();
        bbd.bankOid = BangServer.bankmgr.getPlaceObject().getOid();
        bbd.ranchOid = BangServer.ranchmgr.getPlaceObject().getOid();
        bbd.barberOid = BangServer.barbermgr.getPlaceObject().getOid();
    }

    @Override // documentation inherited
    protected void sessionDidEnd ()
    {
        super.sessionDidEnd();

        // clear out our handle to player object registration
        PlayerObject user = (PlayerObject)_clobj;
        if (user.handle != null) {
            BangServer.clearPlayer(user);
        }

        // this session is over, make a note of it
        recordEndedSession();
    }

    /**
     * Records to logs and the database anything that needs recording at
     * the end of a session.
     */
    protected void recordEndedSession ()
    {
        final PlayerObject user = (PlayerObject)_clobj;
        String uname = "recordEndedSession:" + user.username;
        BangServer.invoker.postUnit(new Invoker.Unit(uname) {
            public boolean invoke () {
                performDatabaseSaves(user);
                return false;
            }
        });
    }

    /**
     * This method is called on the invoker thread and writes to the
     * database any necessary information at the end of a player's
     * session.
     */
    protected void performDatabaseSaves (PlayerObject user)
    {
        try {
            // write out any modified stats
            Stat[] stats = new Stat[user.stats.size()];
            user.stats.toArray(stats);
            BangServer.statrepo.writeModified(user.playerId, stats);

            // write out any modified looks
            for (Iterator iter = user.looks.iterator(); iter.hasNext(); ) {
                Look look = (Look)iter.next();
                if (look.modified) {
                    BangServer.lookrepo.updateLook(user.playerId, look);
                }
            }

            // record our playtime to the database and potentially update our
            // poses
            boolean[] changed = new boolean[_startPoses.length];
            for (int ii = 0; ii < changed.length; ii++) {
                changed[ii] = !ObjectUtil.equals(
                    _startPoses[ii], user.poses[ii]);
            }
            BangServer.playrepo.noteSessionEnded(
                user.playerId, user.poses, changed,
                (int)Math.round(_connectTime / 60f));

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to note ended session " +
                    "[user=" + user.who() + "].", e);
        }
    }

    protected String[] _startPoses;
}
