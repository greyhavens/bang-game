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

    // TODO: add shot type if it becomes necessary

    /** The x coordinate of the fired shot. */
    public short x;

    /** The y coordinate of the fired shot. */
    public short y;

    /** Creates a shot. */
    public Shot (int shooterId, short x, short y)
    {
        this.shooterId = shooterId;
        this.x = x;
        this.y = y;
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
