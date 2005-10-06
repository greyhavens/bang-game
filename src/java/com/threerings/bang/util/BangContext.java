//
// $Id$

package com.threerings.bang.util;

import com.threerings.cast.CharacterManager;

import com.threerings.parlor.util.ParlorContext;

import com.threerings.bang.data.PlayerObject;

/**
 * Defines additional services needed by the Bang! game client.
 */
public interface BangContext extends BasicContext, ParlorContext
{
    /** Returns a reference to our character manager. */
    public CharacterManager getCharacterManager ();

    /** Returns a reference to the current player's user object. Only
     * valid when we are logged onto the server. */
    public PlayerObject getUserObject ();
}
