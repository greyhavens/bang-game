//
// $Id$

package com.threerings.bang.server;

import com.threerings.crowd.data.TokenRing;
import com.threerings.crowd.server.CrowdClient;

import com.threerings.bang.data.BangUserObject;

import static com.threerings.bang.Log.log;

/**
 * Extends {@link CrowdClient} and customizes it for Bang! Howdy.
 */
public class BangClient extends CrowdClient
{
    @Override // documentation inherited
    protected void sessionWillStart ()
    {
        super.sessionWillStart();

        // if we have auth data in the form of a token ring, use it (we
        // can set things directly here rather than use the setter methods
        // because the user object is not yet out in the wild)
        BangUserObject user = (BangUserObject)_clobj;
        if (_authdata instanceof Object[]) {
            Object[] data = (Object[])_authdata;
            user.userId = (Integer)data[0];
            user.tokens = (TokenRing)data[1];
        } else {
            log.warning("Lacking authdata [who=" + _username + "].");
            // otherwise give them zero privileges
            user.userId = -1;
            user.tokens = new TokenRing();
        }
    }
}
