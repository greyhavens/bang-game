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

    @Override // from GangGood
    public int getCoinCost (GangObject gang)
    {
        byte weightClass = gang.getWeightClass();
        if (weightClass >= _weightClass) {
            return 0;
        }
        return _coinCost - GangCodes.WEIGHT_CLASSES[weightClass].coins;
    }

    @Override // from GangGood
    public int getAceCost (GangObject gang)
    {
        byte weightClass = gang.getWeightClass();
        if (weightClass >= _weightClass) {
            return 0;
        }
        return _aceCost - GangCodes.WEIGHT_CLASSES[weightClass].aces;
    }

    /**
     * Returns the new weight class.
     */
    public byte getWeightClass ()
    {
        return _weightClass;
    }

    // documentation inherited
    public boolean isAvailable (GangObject gang)
    {
        return (gang.getWeightClass() != _weightClass &&
                gang.members.size() <= GangCodes.WEIGHT_CLASSES[_weightClass].maxMembers);
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
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, MessageBundle.tcompose(
                    "m.weight_class_upgrade_tip",
                    GangCodes.WEIGHT_CLASSES[_weightClass].maxMembers));
    }

    protected byte _weightClass;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_type", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_townId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_scripCost", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_coinCost", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_priority", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangGood.class, "_aceCost", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.WeightClassUpgradeGood.class, "_weightClass", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_type", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_townId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_scripCost", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_coinCost", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_priority", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangGood.class, "_aceCost", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.WeightClassUpgradeGood.class, "_weightClass", this, out);
    }
}
