//
// $Id$

package com.threerings.bang.game.data;

import java.util.HashSet;

import com.jme.util.export.Savable;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.StatType;

/**
 * Defines a particular additional criterion for "winning" a bounty game.
 */
public abstract class Criterion extends SimpleStreamableObject
    implements Savable
{
    /** Used to report criteria state during a game. */
    public enum State { NOT_MET, MET, COMPLETE, FAILED };

    /**
     * Returns a string describing this criterion for display before the game. This will be
     * translated using the {@link GameCodes#GAME_MSGS} bundle.
     */
    public abstract String getDescription ();

    /**
     * Instructs this criterion to add any stats that should be made watchable during a bounty game
     * to the supplied set.
     */
    public abstract void addWatchedStats (HashSet<StatType> stats);

    /**
     * Returns the current state of this criterion. This is called throughout the game after each
     * tick and as stats are updated.
     */
    public State getCurrentState (BangObject bangobj, int rank)
    {
        return isMet(bangobj, rank) ? State.MET : State.NOT_MET;
    }

    /**
     * Returns the current value of this criterion's underlying statistic. This is called
     * throughout the game after each tick and as stats are updated. The returned value will be
     * translated using the {@link GameCodes#GAME_MSGS} bundle.
     */
    public abstract String getCurrentValue (BangObject bangobj, int rank);

    /**
     * Returns true if this criteron is met, false if not.
     */
    public abstract boolean isMet (BangObject bangobj, int rank);
}
