//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.renderer.Renderer;

import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Insets;

import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;

import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;

import com.threerings.bang.client.bui.PaletteIcon;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.util.BangContext;
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
             ctx.xlate(BangCodes.UNITS_MSGS, config.getName()));
    }

    public UnitIcon (BasicContext ctx, int itemId, UnitConfig config,
                     String name)
    {
        _itemId = itemId;
        _config = config;
        _ctx = ctx;
        setText(name);
        setIcon(BangUI.getUnitIcon(config));
        String msg = MessageBundle.compose(
            "m.unit_icon", config.getName(), config.getName() + "_descrip");
        setTooltipText(ctx.xlate(BangCodes.UNITS_MSGS, msg));
    }

    public void displayAvail (BangContext ctx, boolean disableUnavail)
    {
        // determine whether we should display a locked or unlocked icon over
        // our unit image
        if (_config.badgeCode != 0) {
            String type = "unlocked";
            if (!_config.hasAccess(ctx.getUserObject())) {
                type = "locked";
                setTooltipText(getTooltipText() + "\n\n" + 
                               ctx.xlate(BangCodes.UNITS_MSGS,
                                         _config.getName() + "_badge"));
                setEnabled(!disableUnavail);
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
    protected Insets getTextInsets ()
    {
        return new Insets(5, 5, 5, 0);
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        if (_lock != null) {
            _lock.render(renderer, getWidth()-_lock.getWidth(), 0, _alpha);
        }
    }

    @Override // documentation inherited
    protected BComponent createTooltipComponent (String tiptext)
    {
        BContainer tooltip = GroupLayout.makeVBox(GroupLayout.CENTER);
        tooltip.add(super.createTooltipComponent(tiptext));
        UnitBonus ubonus = new UnitBonus(_ctx);
        ubonus.setUnitConfig(_config, false);
        tooltip.add(ubonus);
        return tooltip;
    }

    protected int _itemId;
    protected UnitConfig _config;
    protected BImage _lock;
    protected BasicContext _ctx;
}
