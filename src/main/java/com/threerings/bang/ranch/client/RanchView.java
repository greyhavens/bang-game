//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.Arrays;

import com.jmex.bui.BLabel;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.ranch.data.RanchCodes;
import com.threerings.bang.ranch.data.RanchObject;

/**
 * Displays the main ranch interface wherein a player's Big Shot units can
 * be inspected, new Big Shots can be browsed and purchased and normal
 * units can also be inspected.
 */
public class RanchView extends ShopView
{
    public RanchView (BangContext ctx)
    {
        super(ctx, RanchCodes.RANCH_MSGS);

        // add our various interface components
        add(new BLabel(_msgs.get("m.intro_tip"), "shop_status"),
            new Rectangle(232, 661, 570, 35));

        String townId = _ctx.getUserObject().townId;
        add(new BLabel(_msgs.get("m.name_" + townId), "shopkeep_name_label"),
            new Rectangle(12, 513, 155, 25));

        add(new WalletLabel(_ctx, true), new Rectangle(25, 53, 150, 40));
        add(createHelpButton(), new Point(780, 25));
        add(new TownButton(ctx), new Point(870, 25));
        add(_status = new StatusLabel(ctx), new Rectangle(250, 10, 520, 50));
        _status.setStyleClass("shop_status");

        _inspector = new UnitInspector(_ctx);
        add(_inspector, new Rectangle(480, 92, 521, 560));

        // create our bigshot and normal unit palettes
        _bigshots = new UnitPalette(ctx, _inspector, COLS, ROWS);
        UnitConfig[] units = UnitConfig.getTownUnits(townId, UnitConfig.Rank.BIGSHOT);
        Arrays.sort(units);
        _bigshots.setBigShots(units, _ctx.getUserObject());

        _units = new UnitPalette(ctx, _inspector, COLS, ROWS);
        units = UnitConfig.getTownUnits(townId, UnitConfig.Rank.NORMAL);
        Arrays.sort(units);
        _units.setUnits(units, false);

        // create our tabs
        add(_tabs = new HackyTabs(ctx, false, "ui/ranch/tab_", TABS, 136, 17) {
            protected void tabSelected (int index) {
                RanchView.this.selectTab(index);
            }
        }, TABS_RECT);

        // start with a random shop tip
        _status.setStatus(getShopTip(), false);
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
        _inspector.init((RanchObject)plobj);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // shut down our palettes
        _bigshots.shutdown();
        _units.shutdown();
    }

    protected void selectTab (int tabidx)
    {
        UnitPalette newtab;
        switch (tabidx) {
        default:
        case 0: newtab = _bigshots; break;
        case 1: newtab = _units; break;
        }

        if (newtab != _seltab) {
            if (_seltab != null) {
                remove(_seltab);
            }
            add(_seltab = newtab, TAB_RECT);
            newtab.selectFirstIcon();
            _status.setStatus(_msgs.get("m." + TABS[tabidx] + "_tip"), false);
        }
    }

    /**
     * Called by the {@link RecruitDialog} when we've recruited a new Big Shot.
     */
    protected void unitRecruited (int itemId)
    {
        _tabs.selectTab(0);
        _bigshots.selectUnit(itemId);
        _status.setStatus(_msgs.get("m.recruited_bigshot"), false);
    }

    protected HackyTabs _tabs;
    protected UnitInspector _inspector;
    protected UnitPalette _seltab;
    protected UnitPalette _bigshots, _units;
    protected StatusLabel _status;

    protected static final int COLS = 2, ROWS = 3;

    protected static final String[] TABS = { "bigshots", "units" };
    protected static final Rectangle TABS_RECT = new Rectangle(
        166, 585, 15+2*140, 66);
    protected static final Rectangle TAB_RECT = new Rectangle(
        188, 83, UnitIcon.ICON_SIZE.width*2, UnitIcon.ICON_SIZE.height*3+35);
}
