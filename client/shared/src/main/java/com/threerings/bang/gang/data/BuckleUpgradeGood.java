//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BuckleUpgrade;
import com.threerings.bang.data.Item;

/**
 * A buckle upgrade for sale.
 */
public class BuckleUpgradeGood extends GangGood
{
    /**
     * Creates a good representing the specified upgrade.
     */
    public BuckleUpgradeGood (int icons, int scripCost, int coinCost, int aceCost)
    {
        super("upgrades/buckle_" + icons, scripCost, coinCost, aceCost);
        _icons = (byte)icons;
    }

    /** A constructor only used during serialization. */
    public BuckleUpgradeGood ()
    {
    }

    // documentation inherited
    public boolean isAvailable (GangObject gang)
    {
        return (gang.getMaxBuckleIcons() < _icons);
    }

    @Override // from Good
    public Item createItem (int gangId)
    {
        return new BuckleUpgrade(gangId, _icons);
    }

    @Override // from Good
    public String getName ()
    {
        return BuckleUpgrade.getName(_icons);
    }

    @Override // from Good
    public String getTip ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.buckle_upgrade_tip");
    }

    protected byte _icons;

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
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.BuckleUpgradeGood.class, "_icons", this, ins);
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
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.BuckleUpgradeGood.class, "_icons", this, out);
    }
}
