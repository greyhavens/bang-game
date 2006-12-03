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
     * Returns null if this criteron is met, a string explaining how it was missed if not.
     */
    public abstract String isMet (BangObject bangobj, PlayerObject player);
}
