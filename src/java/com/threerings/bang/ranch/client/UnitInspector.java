//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.EnumSet;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

/**
 * Displays a large (manipulable?) rendering of a unit model and a list of
 * the unit's statistics below.
 */
public class UnitInspector extends BContainer
    implements IconPalette.Inspector
{
    public UnitInspector (BangContext ctx)
    {
        super(GroupLayout.makeVert(GroupLayout.TOP));
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("ranch");
        _umsgs = ctx.getMessageManager().getBundle("units");

        setBorder(new EmptyBorder(5, 5, 5, 5));

        add(_unit = BangUI.createUnitLabel(null));
        _unit.setText("");
        add(_descrip = new BLabel(""));
        add(_astats = new BLabel(""));
        add(_dstats = new BLabel(""));
    }

    /**
     * Returns the item id of the unit being inspected (or -1 if the
     * inspected unit is not an actual unit).
     */
    public int getItemId ()
    {
        return _itemId;
    }

    /**
     * Returns the configuration of the unit being inspected or null if no
     * unit is being inspected.
     */
    public UnitConfig getConfig ()
    {
        return _config;
    }

    /**
     * Configures the unit we're inspecting.
     */
    public void setUnit (int itemId, UnitConfig config)
    {
        _itemId = itemId;
        _config = config;

        BangUI.configUnitLabel(_unit, config);

        StringBuffer abuf = new StringBuffer();
        StringBuffer dbuf = new StringBuffer();
        for (UnitConfig.Mode mode : EnumSet.allOf(UnitConfig.Mode.class)) {
            String key = mode.toString().toLowerCase();
            noteAdjust(abuf, config.damageAdjust[mode.ordinal()], key);
            noteAdjust(dbuf, config.defenseAdjust[mode.ordinal()], key);
        }
        for (UnitConfig.Make make : EnumSet.allOf(UnitConfig.Make.class)) {
            String key = make.toString().toLowerCase();
            int idx = UnitConfig.MODE_COUNT + make.ordinal();
            noteAdjust(abuf, config.damageAdjust[idx], key);
            noteAdjust(dbuf, config.defenseAdjust[idx], key);
        }
        _astats.setText(_msgs.get("m.attack", abuf.toString()));
        _dstats.setText(_msgs.get("m.defense", dbuf.toString()));
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconSelected (SelectableIcon icon)
    {
        UnitIcon uicon = (UnitIcon)icon;
        setUnit(uicon.getItemId(), uicon.getUnit());
    }

    protected void noteAdjust (StringBuffer buf, int adjust, String key)
    {
        String msg = null;
        if (adjust > 0) {
            msg = MessageBundle.compose("m.good_vs", "m." + key);
        } else if (adjust < 0) {
            msg = MessageBundle.compose("m.bad_vs", "m." + key);
        }
        if (msg != null) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(_umsgs.xlate(msg));
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs, _umsgs;

    protected int _itemId = -1;
    protected UnitConfig _config;

    protected BLabel _unit;
    protected BLabel _descrip;
    protected BLabel _astats, _dstats;
}
