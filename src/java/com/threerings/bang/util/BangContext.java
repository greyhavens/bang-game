//
// $Id$

package com.threerings.bang.util;

import com.threerings.parlor.util.ParlorContext;

import com.threerings.bang.client.BangClient;
import com.threerings.bang.data.PlayerObject;

/**
 * Defines additional services needed by the Bang! game client.
 */
public interface BangContext extends BasicContext, ParlorContext
{
    /** Returns a reference to the main client instance which handles global
     * client things. */
    public BangClient getBangClient();

    /** Returns a reference to the current player's user object. Only
     * valid when we are logged onto the server. */
    public PlayerObject getUserObject ();
}
