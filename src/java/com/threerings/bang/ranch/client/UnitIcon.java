//
// $Id$

package com.threerings.bang.ranch.client;

import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BasicContext;

/**
 * Displays a static view of a unit model for use as an icon in interface
 * displays.
 */
public class UnitIcon extends PaletteIcon
{
    public UnitIcon (BasicContext ctx, int itemId, UnitConfig config)
    {
        this(ctx, itemId, config,
             ctx.xlate(BangCodes.UNITS_MSGS, config.getName()), null);
    }

    public UnitIcon (BasicContext ctx, int itemId, UnitConfig config,
                     String name, PlayerObject player)
    {
        _itemId = itemId;
        _config = config;
        setText(name);
        setIcon(BangUI.getUnitIcon(config));
        String msg = MessageBundle.compose(
            "m.unit_icon", config.getName(), config.getName() + "_descrip");
        setTooltipText(ctx.xlate(BangCodes.UNITS_MSGS, msg));

        // if we were supplied with a player object; disable ourselves if they
        // do not have access to this unit (and tack on the units' badge
        // requirement tip to our tooltip
        if (player != null && _config.badgeCode != 0 &&
            !player.holdsBadge(Badge.getType(_config.badgeCode))) {
            setEnabled(false);
            setAlpha(0.5f);
            setTooltipText(getTooltipText() + "\n\n" + 
                           ctx.xlate(BangCodes.UNITS_MSGS,
                                     config.getName() + "_badge"));
        }
    }

    public int getItemId ()
    {
        return _itemId;
    }

    public UnitConfig getUnit ()
    {
        return _config;
    }

    protected int _itemId;
    protected UnitConfig _config;
}
