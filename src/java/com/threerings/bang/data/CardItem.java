//
// $Id$

package com.threerings.bang.data;

/**
 * Represents a card held in the user's inventory.
 */
public class CardItem extends Item
{
    /** A blank constructor used during unserialization. */
    public CardItem ()
    {
    }

    /** Creates a new card item of the specified type. */
    public CardItem (int ownerId, String type)
    {
        super(ownerId);
        _type = type;
    }

    /** Returns the type code for this Big Shot. This is the same as the
     * associated unit type. */
    public String getType ()
    {
        return _type;
    }

    protected String _type;
}
