//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

/**
 * Provides access to a particular type of unit in games.
 */
public class UnitPass extends Item
{
    /** Creates a pass for the specified unit. */
    public UnitPass (int ownerId, String unit)
    {
        super(ownerId);
        _unit = unit;
    }

    /** A default constructor used for serialization. */
    public UnitPass ()
    {
    }

    /**
     * Returns the type of unit to which this pass gives access.
     */
    public String getUnitType ()
    {
        return _unit;
    }

    @Override // documentation inherited
    public String getName ()
    {
        return MessageBundle.qualify(
            BangCodes.UNITS_MSGS, "m." + _unit + "_pass");
    }

    @Override // documentation inherited
    public String getTooltip ()
    {
        String msg = MessageBundle.qualify(BangCodes.UNITS_MSGS, "m." + _unit);
        msg = MessageBundle.compose("m.unit_pass_tip", msg);
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "goods/passes/" + _unit + ".png";
    }

    protected String _unit;
}
