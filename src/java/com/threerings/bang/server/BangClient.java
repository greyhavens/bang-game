//
// $Id$

package com.threerings.bang.server;

import java.util.logging.Level;

import com.samskivert.util.Invoker;

import com.threerings.crowd.data.TokenRing;
import com.threerings.crowd.server.CrowdClient;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangUserObject;

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
        BangUserObject user = (BangUserObject)_clobj;
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

        // TEMP: start all players in frontier town for now
        user.townId = BangCodes.FRONTIER_TOWN;
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
        final BangUserObject user = (BangUserObject)_clobj;
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
    protected void performDatabaseSaves (BangUserObject user)
    {
        try {
            // record our playtime to the database
            BangServer.playrepo.noteSessionEnded(
                user.playerId, (int)Math.round(_connectTime / 60f));
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to note ended session " +
                    "[user=" + user.who() + "].", e);
        }
    }
}
