//
// $Id$

package com.threerings.bang.game.data;

import com.jme.util.export.Savable;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.PlayerObject;

/**
 * Defines a particular additional criterion for "winning" a bounty game.
 */
public abstract class Criterion extends SimpleStreamableObject
    implements Savable
{
    /**
     * Returns null if this criteron is met, a string explaining how it was missed if not.
     */
    public abstract String isMet (BangObject bangobj, PlayerObject player);

    /**
     * Temporary debugging method for reporting the stats for a met criterion.
     */
    public abstract String reportMet (BangObject bangobj, PlayerObject player);
}
