//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.EnumSet;

import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BContainer;
import com.jmex.bui.BTabbedPane;
import com.jmex.bui.BWindow;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.util.MessageBundle;

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
public class RanchView extends BWindow
    implements PlaceView
{
    public RanchView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), new BorderLayout(5, 5));
        setStyleClass("main_view");

        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);

        final MessageBundle msgs = ctx.getMessageManager().getBundle("ranch");

        // at the top we have a box that displays text and the unit inspector
        BContainer signcont = new BContainer(new BorderLayout(0, 0));
        add(signcont, BorderLayout.NORTH);

        // the sign displays text and unit details
        _sign = new SignView(ctx);
        signcont.add(_sign, BorderLayout.CENTER);

        // center panel: tabbed view with...
        _tabs = new BTabbedPane(GroupLayout.CENTER) {
            public void selectTab (int tabidx) {
                super.selectTab(tabidx);
                _sign.setText(msgs.get("m." + TAB[tabidx] + "_tip"));
            };
        };
        add(_tabs, BorderLayout.CENTER);

        // ...recruited big shots...
        _bigshots = new UnitPalette(ctx, _sign, 4);
        _bigshots.setUser(_ctx.getUserObject());
        _tabs.addTab(msgs.get("t.bigshots"), _bigshots);

        // ...recruitable big shots...
        String townId = _ctx.getUserObject().townId;
        _recruits = new UnitPalette(ctx, _sign, 4);
        _recruits.setUnits(UnitConfig.getTownUnits(
                               townId, UnitConfig.Rank.BIGSHOT));
        _tabs.addTab(msgs.get("t.recruits"), _recruits);

        // ...and normal + special units
        _units = new UnitPalette(ctx, _sign, 4);
        EnumSet<UnitConfig.Rank> ranks = EnumSet.of(
            UnitConfig.Rank.NORMAL, UnitConfig.Rank.SPECIAL);
        _units.setUnits(UnitConfig.getTownUnits(townId, ranks));
        _tabs.addTab(msgs.get("t.units"), _units);

        // add a row displaying our cash on hand and the back button
        BContainer bottom = new BContainer(GroupLayout.makeHStretch());
        add(bottom, BorderLayout.SOUTH);

        bottom.add(new WalletLabel(ctx, false));
        bottom.add(new TownButton(ctx), GroupLayout.FIXED);

        // start out with some special welcome text
        _sign.setText(msgs.get("m.welcome"));
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _sign.init((RanchObject)plobj);
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

    protected BangContext _ctx;
    protected SignView _sign;
    protected BTabbedPane _tabs;
    protected UnitPalette _bigshots, _units, _recruits;

    protected static final String[] TAB = { "bigshots", "recruits", "units" };
}
