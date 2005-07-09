//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.game.data.Terrain;

/**
 * Represents a temporary influence on a unit. Influences can adjust a
 * unit's various basic properties and they can expire after a certain
 * number of ticks.
 */
public abstract class Influence extends SimpleStreamableObject
{
    /** Returns true if this influence has expired. */
    public boolean isExpired (short tick)
    {
        return tick > _startTick + duration();
    }

    /**
     * Adjusts a piece's ticks per move. The default is no adjustment.
     */
    public int adjustTicksPerMove (int ticksPerMove)
    {
        return ticksPerMove;
    }

    /**
     * Adjusts a piece's move distance. The default is no adjustment.
     */
    public int adjustMoveDistance (int moveDistance)
    {
        return moveDistance;
    }

    /**
     * Adjusts a piece's fire distance. The default is no adjustment.
     */
    public int adjustFireDistance (int fireDistance)
    {
        return fireDistance;
    }

    /**
     * Adjusts a piece's sight distance. The default is no adjustment.
     */
    public int adjustSightDistance (int sightDistance)
    {
        return sightDistance;
    }

    /**
     * Adjusts a piece's traversal cost. The default is no adjustment.
     */
    public int adjustTraversalCost (Terrain terrain, int traversalCost)
    {
        return traversalCost;
    }

    /** Configures an influence instance with its starting tick. */
    protected void init (short tick)
    {
        _startTick = tick;
    }

    /** Returns the duration of this influence in ticks. The default is
     * to remain active permanently. */
    protected int duration ()
    {
        return Short.MAX_VALUE;
    }

    protected short _startTick;
}
