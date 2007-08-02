//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.gang.data.GangCodes;

import com.threerings.bang.data.BangCodes;

/**
 * Upgrades a gang's weight class.
 */
public class WeightClassUpgrade extends Item
{
    /**
     * Returns the name of the upgrade to the specified class.
     */
    public static String getName (byte weightClass)
    {
        String cstr = MessageBundle.qualify(GangCodes.GANG_MSGS,
            "m.weight_class." + weightClass);
        return MessageBundle.qualify(BangCodes.GOODS_MSGS,
            MessageBundle.compose("m.weight_class_upgrade", cstr));
    }

    /** A blank constructor used during unserialization. */
    public WeightClassUpgrade ()
    {
    }

    /**
     * Creates a new upgrade to the specified weight class.
     */
    public WeightClassUpgrade (int ownerId, byte weightClass)
    {
        super(ownerId);
        _weightClass = weightClass;

        // these always belong to gangs
        setGangOwned(true);
    }

    /**
     * Returns the weight class of the upgrade.
     */
    public byte getWeightClass ()
    {
        return _weightClass;
    }

    @Override // from Item
    public String getName ()
    {
        return getName(_weightClass);
    }

    @Override // from Item
    public String getTooltip (PlayerObject user)
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.weight_class_upgrade_tip");
    }

    @Override // from Item
    public String getIconPath ()
    {
        return "goods/upgrades/weight_class_" + _weightClass + ".png";
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        return super.isEquivalent(other) &&
            ((WeightClassUpgrade)other)._weightClass == _weightClass;
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
        buf.append(", weightClass").append(_weightClass);
    }

    protected byte _weightClass;
}
