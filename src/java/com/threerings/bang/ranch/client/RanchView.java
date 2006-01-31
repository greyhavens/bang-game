//
// $Id$

package com.threerings.bang.ranch.client;

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
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

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
        super(ctx, "ranch");

        // add our various interface components
        add(new BLabel(_msgs.get("m.intro_tip"), "shop_status"),
            new Rectangle(232, 640, 570, 35));

        add(new WalletLabel(_ctx, true), new Rectangle(40, 78, 150, 40));
        add(createHelpButton(), new Point(780, 25));
        add(new TownButton(ctx), new Point(870, 25));

        _inspector = new UnitInspector(_ctx);
        add(_inspector, new Rectangle(700, 100, 300, 500));

        // center panel: tabbed view with...
        add(_tabs = new BTabbedPane(GroupLayout.CENTER),
            new Rectangle(0, 100, 700, 500));

        // ...recruited big shots...
        _bigshots = new UnitPalette(ctx, _inspector, 4);
        _bigshots.setUser(_ctx.getUserObject());
        _tabs.addTab(_msgs.get("t.bigshots"), _bigshots);

        // ...recruitable big shots...
        String townId = _ctx.getUserObject().townId;
        _recruits = new UnitPalette(ctx, _inspector, 4);
        _recruits.setUnits(UnitConfig.getTownUnits(
                               townId, UnitConfig.Rank.BIGSHOT));
        _tabs.addTab(_msgs.get("t.recruits"), _recruits);

        // ...and normal + special units
        _units = new UnitPalette(ctx, _inspector, 4);
        EnumSet<UnitConfig.Rank> ranks = EnumSet.of(
            UnitConfig.Rank.NORMAL, UnitConfig.Rank.SPECIAL);
        _units.setUnits(UnitConfig.getTownUnits(townId, ranks));
        _tabs.addTab(_msgs.get("t.units"), _units);
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _inspector.init((RanchObject)plobj);
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
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

    /**
     * Called by the {@link SignView} when we've recruited a new Big Shot.
     */
    protected void unitRecruited (int itemId)
    {
        _tabs.selectTab(0);
        _bigshots.selectUnit(itemId);
    }

    protected UnitInspector _inspector;
    protected BTabbedPane _tabs;
    protected UnitPalette _bigshots, _units, _recruits;

    protected static final String[] TAB = { "bigshots", "recruits", "units" };
}
