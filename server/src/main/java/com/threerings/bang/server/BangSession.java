//
// $Id$

package com.threerings.bang.server;

import java.util.List;

import com.google.inject.Inject;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;

import com.threerings.presents.annotation.AuthInvoker;
import com.threerings.presents.net.BootstrapData;
import com.threerings.crowd.server.CrowdSession;

import com.threerings.bang.admin.server.BangAdminManager;
import com.threerings.bang.server.persist.BangStatRepository;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.server.persist.LookRepository;

import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.BangCredentials;
import com.threerings.bang.data.BangTokenRing;
import com.threerings.bang.data.PlayerObject;

import static com.threerings.bang.Log.log;

/**
 * Extends {@link CrowdSession} and customizes it for Bang! Howdy.
 */
public class BangSession extends CrowdSession
{
    @Override // documentation inherited
    public void shutdown ()
    {
        super.shutdown();

        // this session is over, make a note of it
        recordEndedSession();
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
        if (((PlayerObject)_clobj).tokens.isSupport()) {
            bbd.statusOid = _adminmgr.statobj.getOid();
        }
        bbd.townOid = BangServer.townobj.getOid();
        bbd.saloonOid = BangServer.saloonmgr.getPlaceObject().getOid();
        bbd.storeOid = BangServer.storemgr.getPlaceObject().getOid();
        bbd.ranchOid = BangServer.ranchmgr.getPlaceObject().getOid();
        bbd.barberOid = BangServer.barbermgr.getPlaceObject().getOid();
        bbd.stationOid = BangServer.stationmgr.getPlaceObject().getOid();
        bbd.hideoutOid = BangServer.hideoutmgr.getPlaceObject().getOid();
        bbd.officeOid = BangServer.officemgr.getPlaceObject().getOid();
        bbd.tourniesOid = BangServer.tournmgr.getTourniesObject().getOid();
    }

    @Override // documentation inherited
    protected void sessionWillStart ()
    {
        super.sessionWillStart();
        PlayerObject user = (PlayerObject)_clobj;

        // generate an audit log entry
        final BangCredentials creds = (BangCredentials)getCredentials();
        BangServer.generalLog("session_start " + user.playerId + " ip:" + getInetAddress() +
                " id:" + creds.ident + " node:" + ServerConfig.nodename +
                " sid:" + creds.affiliate);

        // register the player with their handle
        BangServer.locator.registerPlayer(user);

        // if we have auth data in the form of a token ring, use it
        if (_authdata instanceof BangTokenRing) {
            BangTokenRing atokens = (BangTokenRing)_authdata;
            // add tokens provided by our authentication plugin; we can set things directly here
            // because the user object is not yet out in the wild
            for (int ii = 0; ii < 31; ii++) {
                int token = (1 << ii);
                if (atokens.holdsToken(token)) {
                    user.tokens.setToken(token);
                }
            }
        } else {
            log.warning("Missing or bogus authdata", "who", _authname, "adata", _authdata);
        }

        // configure the player in the town for this server
        user.townId = ServerConfig.townId;

        // configure anonimity
        user.tokens.setToken(BangTokenRing.ANONYMOUS, creds.anonymous);

        // make a note of their current avatar poses for later comparison and potential updating
        _startPoses = user.poses.clone();

        // check to see if this player has any rewards and redeem them if so
        _authInvoker.postUnit(new Invoker.Unit("redeemRewards:" + _authname) {
            public boolean invoke () {
                _rewards = BangServer.author.redeemRewards(_authname.toString(), creds.ident);
                return (_rewards.size() > 0);
            }
            public void handleResult () {
                BangServer.playmgr.redeemRewards((PlayerObject)_clobj, _rewards);
            }
            protected List<String> _rewards;
        });
    }

    @Override // documentation inherited
    protected void sessionWillResume ()
    {
        super.sessionWillResume();

        // generate an audit log entry
        BangServer.generalLog("session_resume " + ((PlayerObject)_clobj).playerId +
                              " ip:" + getInetAddress());
    }

    @Override // documentation inherited
    protected void sessionDidEnd ()
    {
        super.sessionDidEnd();

        // clear out our handle to player object registration
        PlayerObject user = (PlayerObject)_clobj;
        BangServer.locator.clearPlayer(user);

        // this session is over, make a note of it
        recordEndedSession();

        // generate an audit log recording this session
        BangServer.generalLog("session_end " + user.playerId + " ctime:" + _connectTime +
                " node:" + ServerConfig.nodename);
    }

    /**
     * Records to logs and the database anything that needs recording at the end of a session.
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
     * This method is called on the invoker thread and writes to the database any necessary
     * information at the end of a player's session.
     */
    protected void performDatabaseSaves (PlayerObject user)
    {
        try {
            // write out any modified stats
            _statrepo.writeModified(user.playerId, user.stats.toArrayList());

            // write out any modified looks
            boolean updatedWanted = false;
            for (Look look : user.looks) {
                if (look.modified) {
                    _lookrepo.updateLook(user.playerId, look);
                    // if this their "wanted poster" look; generate snapshot
                    if (user.getLook(Look.Pose.WANTED_POSTER) == look) {
                        updatedWanted = true;
                    }
                }
            }

            // record our playtime to the database and potentially update our poses
            boolean[] changed = new boolean[_startPoses.length];
            for (int ii = 0; ii < changed.length; ii++) {
                changed[ii] = !ObjectUtil.equals(_startPoses[ii], user.poses[ii]);
            }
            _playrepo.noteSessionEnded(
                user.playerId, user.poses, changed, Math.round(_connectTime / 60f));

            // if our wanted poster look changed, generate a new snapshot
            if (updatedWanted || changed[Look.Pose.WANTED_POSTER.ordinal()]) {
                Look look = user.getLook(Look.Pose.WANTED_POSTER);
                int[] print;
                if (look != null && (print = look.getAvatar(user).print) != null) {
                    _lookrepo.updateSnapshot(user.playerId, print);
                }
            }

        } catch (Exception e) {
            log.warning("Failed to note ended session", "user", user.who(), e);
        }
    }

    protected String[] _startPoses;

    // dependencies
    @Inject protected @AuthInvoker Invoker _authInvoker;
    @Inject protected BangAdminManager _adminmgr;
    @Inject protected PlayerRepository _playrepo;
    @Inject protected BangStatRepository _statrepo;
    @Inject protected LookRepository _lookrepo;
}
