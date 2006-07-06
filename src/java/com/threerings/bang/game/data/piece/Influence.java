//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.TerrainConfig;

import com.threerings.bang.game.client.effect.InfluenceViz;

/**
 * Represents a temporary influence on a unit. Influences can adjust a
 * unit's various basic properties and they can expire after a certain
 * number of ticks.
 */
public abstract class Influence extends SimpleStreamableObject
{
    /** Returns the name of this influence, used for icons and
     * messages (can be <code>null</code> for none). */
    public abstract String getName ();

    /** Creates a visualization for the influence, or returns <code>null</code>
     * for none. */
    public InfluenceViz createViz ()
    {
        return null;
    }
    
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
     * Adjusts a piece's minimum fire distance. The default is no
     * adjustment.
     */
    public int adjustMinFireDistance (int fireDistance)
    {
        return fireDistance;
    }

    /**
     * Adjusts a piece's maximum fire distance. The default is no
     * adjustment.
     */
    public int adjustMaxFireDistance (int fireDistance)
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
    public int adjustTraversalCost (TerrainConfig terrain, int traversalCost)
    {
        return traversalCost;
    }

    /**
     * Adjusts the damage a piece does to another piece.
     */
    public int adjustAttack (Piece target, int damage)
    {
        return damage;
    }

    /**
     * Adjusts the damage a piece receives from another piece.
     */
    public int adjustDefend (Piece shooter, int damage)
    {
        return damage;
    }

    /**
     * Returns true if the last call to adjustAttack caused a change
     * in attack.  The default is false.
     */
    public boolean didAdjustAttack ()
    {
        return false;
    }

    /**
     * Returns true if the last call to adjustDefend caused a change
     * in defense.  The default is false.
     */
    public boolean didAdjustDefend ()
    {
        return false;
    }

    @Override // documentation inherited
    public String toString ()
    {
        return getName();
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
