//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.gang.data.GangCodes;

import com.threerings.bang.data.BangCodes;

/**
 * Upgrades a gang's buckle (to allow more icons).
 */
public class BuckleUpgrade extends Item
{
    /**
     * Returns the name of the buckle with the specified number of icons.
     */
    public static String getName (int icons)
    {
        return MessageBundle.qualify(GangCodes.GANG_MSGS,
            MessageBundle.compose("m.buckle_icons", "m.buckle_icons." + icons));
    }

    /** A blank constructor used during unserialization. */
    public BuckleUpgrade ()
    {
    }

    /**
     * Creates a new upgrade to the specified buckle configuration.
     */
    public BuckleUpgrade (int ownerId, byte icons)
    {
        super(ownerId);
        _icons = icons;

        // these always belong to gangs
        setGangOwned(true);
    }

    /**
     * Returns the number of icons allowed on the buckle.
     */
    public byte getIcons ()
    {
        return _icons;
    }

    @Override // from Item
    public String getName ()
    {
        return getName(_icons);
    }

    @Override // from Item
    public String getTooltip (PlayerObject user)
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.buckle_upgrade_tip");
    }

    @Override // from Item
    public String getIconPath ()
    {
        return "goods/upgrades/buckle_" + _icons + ".png";
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        return super.isEquivalent(other) &&
            ((BuckleUpgrade)other)._icons == _icons;
    }

    @Override // documentation inherited
    public boolean canBeOwned (PlayerObject user)
    {
        return false;
    }

    @Override // from Item
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", icons").append(_icons);
    }

    protected byte _icons;
}
