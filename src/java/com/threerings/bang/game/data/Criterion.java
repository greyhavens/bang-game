//
// $Id$

package com.threerings.bang.game.data;

import java.util.HashSet;

import com.jme.util.export.Savable;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.Stat;

/**
 * Defines a particular additional criterion for "winning" a bounty game.
 */
public abstract class Criterion extends SimpleStreamableObject
    implements Savable
{
    /**
     * Returns a string describing this criterion for display before the game.
     */
    public abstract String getDescription ();

    /**
     * Instructs this criterion to add any stats that should be made watchable during a bounty game
     * to the supplied set.
     */
    public abstract void addWatchedStats (HashSet<Stat.Type> stats);

    /**
     * Returns the current state of this criterion (the value of the underlying statistic or
     * related bit of information). This is called throughout the game after each tick and as stats
     * are updated. The returned value will be translated.
     */
    public abstract String getCurrentState (BangObject bangobj, int rank);

    /**
     * Returns true if this criteron is met, false if not.
     */
    public abstract boolean isMet (BangObject bangobj, int rank);
}
