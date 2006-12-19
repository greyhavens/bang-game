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
     * are updated.
     */
    public abstract String getCurrentState (BangObject bangobj);

    /**
     * Returns null if this criteron is met, a string explaining how it was missed if not.
     */
    public abstract String isMet (BangObject bangobj);

    /**
     * Temporary debugging method for reporting the stats for a met criterion.
     */
    public abstract String reportMet (BangObject bangobj);
}
