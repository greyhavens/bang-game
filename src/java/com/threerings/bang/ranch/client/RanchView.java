//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTabbedPane;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.ranch.data.RanchCodes;
import com.threerings.bang.ranch.data.RanchObject;

import static com.threerings.bang.Log.log;

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

        add(new WalletLabel(_ctx, true), new Rectangle(40, 73, 150, 40));
        add(createHelpButton(), new Point(780, 25));
        add(new TownButton(ctx), new Point(870, 25));
        add(_status = new StatusLabel(ctx), new Rectangle(250, 10, 520, 50));
        _status.setStyleClass("shop_status");

        _inspector = new UnitInspector(_ctx);
        add(_inspector, new Rectangle(178, 60, 258, 586));

        // create our various tabs: recruitable big shots...
        _bigshots = new UnitPalette(ctx, _inspector, 4, 3);
        UnitConfig[] units =
            UnitConfig.getTownUnits(townId, UnitConfig.Rank.BIGSHOT);
        Arrays.sort(units, new Comparator<UnitConfig>() {
            public int compare (UnitConfig uc1, UnitConfig uc2) {
                int rv = uc2.getTownId().compareTo(uc1.getTownId());
                return (rv != 0) ? rv : uc1.type.compareTo(uc2.type);
            };
        });
        _bigshots.setUnits(units, false);

        // ...recruited big shots...
        _recruits = new UnitPalette(ctx, _inspector, 4, 3);
        _recruits.setUser(_ctx.getUserObject());

        // ...and normal + special units
        _units = new UnitPalette(ctx, _inspector, 4, 3);
        EnumSet<UnitConfig.Rank> ranks = EnumSet.of(
            UnitConfig.Rank.NORMAL, UnitConfig.Rank.SPECIAL);
        units = UnitConfig.getTownUnits(townId, ranks);
        Arrays.sort(units, new Comparator<UnitConfig>() {
            public int compare (UnitConfig uc1, UnitConfig uc2) {
                int rv = uc2.getTownId().compareTo(uc1.getTownId());
                if (rv != 0) {
                    return rv;
                }
                rv = Math.abs(uc1.badgeCode) - Math.abs(uc2.badgeCode);
                return rv != 0 ? rv : uc1.type.compareTo(uc2.type);
            };
        });
        _units.setUnits(units, false);

        // create our tabs
        add(_tabs = new HackyTabs(ctx, false, "ui/ranch/tab_", TABS, 136, 17) {
            protected void tabSelected (int index) {
                RanchView.this.selectTab(index);
            }
        }, new Rectangle(433, 585, 15+3*140, 66));
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
        _recruits.shutdown();
    }

    protected void selectTab (int tabidx)
    {
        UnitPalette newtab;
        switch (tabidx) {
        default:
        case 0: newtab = _bigshots; break;
        case 1: newtab = _recruits; break;
        case 2: newtab = _units; break;
        }

        if (newtab != _seltab) {
            if (_seltab != null) {
                remove(_seltab);
            }
            add(_seltab = newtab, TAB_LOC);
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
    protected UnitPalette _bigshots, _units, _recruits;
    protected StatusLabel _status;

    protected static final String[] TABS = { "bigshots", "recruits", "units" };
    protected static final Rectangle TAB_LOC = new Rectangle(
        453, 77, UnitIcon.ICON_SIZE.width*4, UnitIcon.ICON_SIZE.height*3+40);
}
