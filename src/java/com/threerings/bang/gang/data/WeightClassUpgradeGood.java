//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.WeightClassUpgrade;

/**
 * A weight class upgrade for sale.
 */
public class WeightClassUpgradeGood extends GangGood
{
    /**
     * Creates a good representing the specified upgrade.
     */
    public WeightClassUpgradeGood (byte weightClass, int scripCost, int coinCost, int aceCost)
    {
        super("upgrades/weight_class_" + weightClass, scripCost, coinCost, aceCost);
        _weightClass = weightClass;
    }

    /** A constructor only used during serialization. */
    public WeightClassUpgradeGood ()
    {
    }

    /**
     * Returns the weight class of this upgrade.
     */
    public byte getWeightClass ()
    {
        return _weightClass;
    }

    // documentation inherited
    public String getIconPath ()
    {
        return "goods/default.png";
    }

    // documentation inherited
    public boolean isAvailable (GangObject gang)
    {
        return (gang.getWeightClass() < _weightClass);
    }

    @Override // from Good
    public Item createItem (int gangId)
    {
        return new WeightClassUpgrade(gangId, _weightClass);
    }

    @Override // from Good
    public String getName ()
    {
        return WeightClassUpgrade.getName(_weightClass);
    }

    @Override // from Good
    public String getTip ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.weight_class_upgrade_tip");
    }

    protected byte _weightClass;
}
