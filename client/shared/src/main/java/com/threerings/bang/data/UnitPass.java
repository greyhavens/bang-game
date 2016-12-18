//
// $Id$

package com.threerings.bang.data;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
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
        return UnitConfig.getName(_unit) + "_pass";
    }

    @Override // documentation inherited
    public String getTooltip (PlayerObject user)
    {
        String msg = UnitConfig.getName(_unit);
        msg = MessageBundle.compose("m.unit_pass_tip", msg);
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "goods/passes/" + _unit + ".png";
    }

    @Override // documentation inherited
    public void unpersistFrom (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        super.unpersistFrom(in);

        // some hackery to deal with old item types
        if (_unit.indexOf("/") == -1) {
            _unit = BangCodes.FRONTIER_TOWN + "/" + _unit;
        }
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        return super.isEquivalent(other) &&
            ((UnitPass)other)._unit.equals(_unit);
    }

    protected String _unit;
}
