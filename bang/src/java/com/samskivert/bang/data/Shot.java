//
// $Id$

package com.samskivert.bang.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Represents a shot fired from one piece at another.
 */
public class Shot extends SimpleStreamableObject
{
    /** The piece id of the shooter. */
    public int shooterId;

    /** The piece id of the target. */
    public int targetId;

    // TODO: add shot type if it becomes necessary

    /** The x coordinate of the fired shot. */
    public short x;

    /** The y coordinate of the fired shot. */
    public short y;

    /** Indicates the points of damage being done by the shot. */
    public int damage;

    /** Creates a shot. */
    public Shot (int shooterId, int targetId, short x, short y, int damage)
    {
        this.shooterId = shooterId;
        this.targetId = targetId;
        this.x = x;
        this.y = y;
        this.damage = damage;
    }

    /** A constructor for unserialization. */
    public Shot ()
    {
    }

    /**
     * Returns true if the specified tile is affected by this shot.
     */
    public boolean affects (short x, short y)
    {
        // if we add area of effect shots, that will plug in here
        return (x == this.x) && (y == this.y);
    }
}
