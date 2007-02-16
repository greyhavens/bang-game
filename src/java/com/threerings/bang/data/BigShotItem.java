//
// $Id$

package com.threerings.bang.data;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;

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
    public String getTooltip (PlayerObject user)
    {
        return UnitConfig.getTip(_type);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "units/" + _type + "/icon.png";
    }

    @Override // documentation inherited
    public void unpersistFrom (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        super.unpersistFrom(in);

        // some hackery to deal with old item types
        if (_type.indexOf("/") == -1) {
            _type = BangCodes.FRONTIER_TOWN + "/" + _type;
        }
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        BigShotItem oshot;
        return super.isEquivalent(other) &&
            (oshot = (BigShotItem)other)._type.equals(_type) &&
            oshot._name.equals(_name);
    }

    protected String _type;
    protected String _name;
}
