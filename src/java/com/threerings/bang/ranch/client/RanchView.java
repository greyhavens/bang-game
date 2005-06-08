//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.renderer.ColorRGBA;

import com.jme.bui.BButton;
import com.jme.bui.BContainer;
import com.jme.bui.BLabel;
import com.jme.bui.BTabbedPane;
import com.jme.bui.BWindow;
import com.jme.bui.border.EmptyBorder;
import com.jme.bui.border.LineBorder;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.BorderLayout;
import com.jme.bui.layout.GroupLayout;
import com.jme.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

/**
 * Displays the main ranch interface wherein a player's Big Shot units can
 * be inspected, new Big Shots can be browsed and purchased and normal
 * units can also be inspected.
 */
public class RanchView extends BWindow
    implements ActionListener
{
    public RanchView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), new BorderLayout(5, 5));
        setBorder(new EmptyBorder(5, 5, 5, 5));
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("ranch");

        // we'll add this later, but the palettes need to know about it
        _inspector = new UnitInspector(ctx);

        // center panel: tabbed view with big shots, units, recruits
        _tabs = new BTabbedPane();
        _tabs.setBorder(new LineBorder(ColorRGBA.black));
        add(_tabs, BorderLayout.CENTER);
        _bigshots = new UnitPalette(ctx, _inspector);
        _tabs.addTab(_msgs.get("t.bigshots"), _bigshots);
        _units = new UnitPalette(ctx, _inspector);
        _units.setUnits(UnitConfig.getTownUnits(BangCodes.FRONTIER_TOWN));
        _tabs.addTab(_msgs.get("t.units"), _units);
        _recruits = new UnitPalette(ctx, _inspector);
        _tabs.addTab(_msgs.get("t.recruits"), _recruits);

        // side panel: unit inspector, customize/recruit and back button
        BContainer side = new BContainer(GroupLayout.makeVStretch());
        side.add(_inspector, GroupLayout.FIXED);
        // TODO: add customize/recruit button
        side.add(new BLabel("")); // absorb space
        BButton btn;
        side.add(btn = new BButton(_msgs.get("m.back_to_town"), "back"),
                 GroupLayout.FIXED);
        btn.addListener(this);
        add(side, BorderLayout.EAST);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("back".equals(event.getAction())) {
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected BTabbedPane _tabs;
    protected UnitPalette _bigshots, _units, _recruits;
    protected UnitInspector _inspector;
}
