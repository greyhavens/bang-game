//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

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
    public void setGivenName (Name name)
    {
        _name = name.toString();
    }

    /** Returns the name given to this Big Shot unit by the player. */
    public Name getGivenName ()
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
    public String getName ()
    {
        return MessageBundle.taint(_name);
    }

    @Override // documentation inherited
    public String getTooltip ()
    {
        String msg = MessageBundle.compose(
            "m.unit_icon", "m." + _type, "m." + _type + "_descrip");
        return MessageBundle.qualify(BangCodes.UNITS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "units/" + _type + "/icon.png";
    }

    protected String _type;
    protected String _name;
}
