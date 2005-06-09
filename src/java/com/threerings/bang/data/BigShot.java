//
// $Id$

package com.threerings.bang.data;

/**
 * Represents a Big Shot unit owned by a player.
 */
public class BigShot extends Item
{
    /**
     * A blank constructor used during unserialization.
     */
    public BigShot ()
    {
    }

    /**
     * Creates a new Big Shot item of the specified type.
     */
    public BigShot (int ownerId, String type)
    {
        super(ownerId);
        _type = type;
    }

    /**
     * Returns the type code for this Big Shot. This is the same as the
     * associated unit type.
     */
    public String getType ()
    {
        return _type;
    }

    protected String _type;
}
