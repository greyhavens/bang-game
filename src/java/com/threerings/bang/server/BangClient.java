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
    }
}
