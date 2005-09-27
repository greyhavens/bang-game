//
// $Id$

package com.threerings.bang.server;

import java.util.logging.Level;

import com.samskivert.util.Invoker;

import com.threerings.crowd.data.TokenRing;
import com.threerings.crowd.server.CrowdClient;
import com.threerings.presents.net.BootstrapData;

import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.BangCodes;
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

        // if we have auth data in the form of a token ring, use it
        PlayerObject user = (PlayerObject)_clobj;
        if (_authdata instanceof TokenRing) {
            // we can set things directly here rather than use the setter
            // methods because the user object is not yet out in the wild
            user.tokens = (TokenRing)_authdata;
        } else {
            log.warning("Missing or bogus authdata [who=" + _username +
                        ", adata=" + _authdata + "].");
            // give them zero privileges
            user.tokens = new TokenRing();
        }

        // configure the player in the town for this server
        user.townId = ServerConfig.getTownId();
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
        bbd.saloonOid = BangServer.saloonmgr.getPlaceObject().getOid();
        bbd.storeOid = BangServer.storemgr.getPlaceObject().getOid();
        bbd.bankOid = BangServer.bankmgr.getPlaceObject().getOid();
        bbd.ranchOid = BangServer.ranchmgr.getPlaceObject().getOid();
    }

    @Override // documentation inherited
    protected void sessionDidEnd ()
    {
        super.sessionDidEnd();

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

            // record our playtime to the database
            BangServer.playrepo.noteSessionEnded(
                user.playerId, (int)Math.round(_connectTime / 60f));

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to note ended session " +
                    "[user=" + user.who() + "].", e);
        }
    }
}
