//
// $Id$

package com.threerings.bang.data;

/**
 * Represents a Big Shot unit owned by a player.
 */
public class BigShotItem extends Item
{
    /** A blank constructor used during unserialization. */
    public BigShotItem ()
    {
    }

    /** Creates a new Big Shot item of the specified type. */
    public BigShotItem (int ownerId, String type)
    {
        super(ownerId);
        _type = type;
    }

    /** Configures the name of this Big Shot unit. */
    public void setName (String name)
    {
        _name = name;
    }

    /** Returns the name of this Big Shot unit. */
    public String getName ()
    {
        return _name;
    }

    /** Returns the type code for this Big Shot. This is the same as the
     * associated unit type. */
    public String getType ()
    {
        return _type;
    }

    protected String _type;
    protected String _name;
}
