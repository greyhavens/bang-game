//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.data.UnitPass;

/**
 * Makes available a pass that provides access to a particular unit.
 */
public class UnitPassGood extends Good
{
    /**
     * Creates a good representing a pass for the specified unit.
     */
    public UnitPassGood (String unit, String townId, int scripCost, int coinCost)
    {
        super(unit + "_pass", townId, scripCost, coinCost, UNIT_PASS_PRIORITY);
    }

    /** A constructor only used during serialization. */
    public UnitPassGood ()
    {
    }

    /**
     * Returns the unit type provided by this pass.
     */
    public String getUnitType ()
    {
        return _type.substring(0, _type.length()-5);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "goods/passes/" + getUnitType() + ".png";
    }

    @Override // documentation inherited
    public String getName ()
    {
        return UnitConfig.getName(getUnitType()) + "_pass";
    }

    @Override // documentation inherited
    public String getTip ()
    {
        String msg = UnitConfig.getName(getUnitType());
        msg = MessageBundle.compose("m.unit_pass_tip", msg);
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // from Good
    public boolean honorsGoldPass ()
    {
        return true;
    }

    @Override // documentation inherited
    public boolean isAvailable (PlayerObject user)
    {
        // make sure the player doesn't already have the badge requirement for this unit or the
        // pass itself
        UnitConfig uc = UnitConfig.getConfig(getUnitType(), true);
        return (uc == null) ? false : !uc.hasAccess(user);
    }

    @Override // documentation inherited
    public Item createItem (int playerId)
    {
        return new UnitPass(playerId, getUnitType());
    }

    @Override // documentation inherited
    public boolean wouldCreateItem (Item item)
    {
        if (!(item instanceof UnitPass)) {
            return false;
        }
        return ((UnitPass)item).getUnitType().equals(getUnitType());
    }

    protected static final int UNIT_PASS_PRIORITY = 10;
}
