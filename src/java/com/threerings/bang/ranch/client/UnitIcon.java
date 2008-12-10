//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.renderer.Renderer;

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
 * Displays a static view of a unit model for use as an icon in interface displays.
 */
public class UnitIcon extends PaletteIcon
{
    public UnitIcon (BasicContext ctx, UnitConfig config)
    {
        _config = config;
        _ctx = ctx;
        setText(ctx.xlate(BangCodes.UNITS_MSGS, config.getName()));
        setIcon(BangUI.getUnitIcon(config));
        String msg = MessageBundle.compose("m.unit_icon", config.getName(), config.getTip());
        setTooltipText(ctx.xlate(BangCodes.UNITS_MSGS, msg));
    }

    public void displayAvail (BangContext ctx, boolean disableUnavail)
    {
        // determine if we should display a locked/unlocked icon over our unit
        if (_config.badgeCode != 0) {
            boolean locked = !_config.hasAccess(ctx.getUserObject());
            setLocked(ctx, locked);
            if (locked) {
                setEnabled(!disableUnavail);
                setTooltipText(getTooltipText() + BADGE_SEP +
                               ctx.xlate(BangCodes.UNITS_MSGS, _config.getName() + "_badge"));
            }
        }
    }

    public void setLocked (BangContext ctx, boolean locked)
    {
        String type = locked ? "locked" : "unlocked";
        _lock = ctx.loadImage("ui/ranch/unit_" + type + ".png");
        if (isAdded()) {
            _lock.reference();
        }
    }

    /**
     * Converts a "recruitable" icon into a "recruited" icon. Clears out any lock icon being
     * displayed.
     */
    public void setItem (int itemId, String name)
    {
        _itemId = itemId;
        setText(_ctx.xlate(BangCodes.UNITS_MSGS, name));

        // clear out our lock
        if (_lock != null) {
            if (isAdded()) {
                _lock.release();
            }
            _lock = null;
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

    @Override // documentation inherited
    protected BComponent createTooltipComponent (String tiptext)
    {
        // do some hackery to display our badge requirements below our bonus/penalty stuff
        String badgetip = null;
        int sepidx = tiptext.indexOf(BADGE_SEP);
        if (sepidx != -1) {
            badgetip = tiptext.substring(sepidx+BADGE_SEP.length());
            tiptext= tiptext.substring(0, sepidx);
        }

        BContainer tooltip = GroupLayout.makeVBox(GroupLayout.CENTER);
        ((GroupLayout)tooltip.getLayoutManager()).setOffAxisJustification(GroupLayout.LEFT);
        ((GroupLayout)tooltip.getLayoutManager()).setGap(15);
        tooltip.add(super.createTooltipComponent(tiptext));
        UnitBonus ubonus = new UnitBonus(_ctx, 25);
        ubonus.setUnitConfig(_config, false, UnitBonus.Which.BOTH);
        tooltip.add(ubonus);

        if (badgetip != null) {
            tooltip.add(super.createTooltipComponent(badgetip));
        }
        return tooltip;
    }

    protected int _itemId = -1;
    protected UnitConfig _config;
    protected BImage _lock;
    protected BasicContext _ctx;

    protected static final String BADGE_SEP = "\n\n";
}
