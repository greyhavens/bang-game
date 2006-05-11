//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.renderer.Renderer;
import com.jmex.bui.BImage;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.PaletteIcon;
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

        // if a player was supplied, determine whether we should display a
        // locked or unlocked icon over our unit image
        if (player != null && _config.badgeCode != 0) {
            String type = "unlocked";
            if (!_config.hasAccess(player)) {
                type = "locked";
                setTooltipText(getTooltipText() + "\n\n" + 
                               ctx.xlate(BangCodes.UNITS_MSGS,
                                         config.getName() + "_badge"));
            }
            _lock = ctx.loadImage("ui/ranch/unit_" + type + ".png");
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

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        if (_lock != null) {
            _lock.reference();
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        if (_lock != null) {
            _lock.release();
        }
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        if (_lock != null) {
            _lock.render(renderer, getWidth()-_lock.getWidth(), 0, _alpha);
        }
    }

    protected int _itemId;
    protected UnitConfig _config;
    protected BImage _lock;
}
