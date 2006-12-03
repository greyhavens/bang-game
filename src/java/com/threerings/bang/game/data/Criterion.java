//
// $Id$

package com.threerings.bang.game.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.PlayerObject;

/**
 * Defines a particular additional criterion for "winning" a bounty game.
 */
public abstract class Criterion extends SimpleStreamableObject
{
    /**
     * Returns true if this criteron is met, false if not.
     */
    public abstract boolean isMet (BangObject bangobj, PlayerObject player);
}
