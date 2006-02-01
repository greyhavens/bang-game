//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.Name;

import com.threerings.bang.client.ItemIcon;

/**
 * Represents a Big Shot unit owned by a player.
 */
public class BigShotItem extends Item
{
    /** The maximum big shot name length. */
    public static final int MAX_NAME_LENGTH = 18;

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
    public void setName (Name name)
    {
        _name = name.toString();
    }

    /** Returns the name of this Big Shot unit. */
    public Name getName ()
    {
        return new Name(_name);
    }

    /** Returns the type code for this Big Shot. This is the same as the
     * associated unit type. */
    public String getType ()
    {
        return _type;
    }

    @Override // documentation inherited
    public ItemIcon createIcon ()
    {
        return null;
    }

    protected String _type;
    protected String _name;
}
