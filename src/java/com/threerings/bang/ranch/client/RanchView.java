//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.renderer.ColorRGBA;

import com.jme.bui.BButton;
import com.jme.bui.BContainer;
import com.jme.bui.BLabel;
import com.jme.bui.BTabbedPane;
import com.jme.bui.BTextField;
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

import static com.threerings.bang.Log.log;

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

        // center panel: tabbed view with...
        _tabs = new BTabbedPane();
        _tabs.setBorder(new LineBorder(ColorRGBA.black));
        add(_tabs, BorderLayout.CENTER);

        // ...recruited big shots...
        _bigshots = new UnitPalette(ctx, _inspector);
        _bigshots.setUser(_ctx.getUserObject());
        _tabs.addTab(_msgs.get("t.bigshots"), _bigshots);

        // ...recruitable big shots...
        _recruits = new UnitPalette(ctx, _inspector);
        _recruits.setUnits(UnitConfig.getTownUnits(BangCodes.FRONTIER_TOWN,
                                                   UnitConfig.Rank.BIGSHOT));
        _tabs.addTab(_msgs.get("t.recruits"), _recruits);

        // ...normal units...
        _units = new UnitPalette(ctx, _inspector);
        _units.setUnits(UnitConfig.getTownUnits(BangCodes.FRONTIER_TOWN,
                                                UnitConfig.Rank.NORMAL));
        _tabs.addTab(_msgs.get("t.units"), _units);

        // TODO: and special units?

        // side panel: unit inspector, customize/recruit and back button
        BContainer side = new BContainer(GroupLayout.makeVStretch());
        side.add(_inspector, GroupLayout.FIXED);
        side.add(_uaction = new BButton(_msgs.get("m.recruit"), "recruit"),
                 GroupLayout.FIXED);
        _uaction.addListener(this);
        side.add(new BLabel("")); // absorb space
        BButton btn;
        side.add(btn = new BButton(_msgs.get("m.back_to_town"), "back"),
                 GroupLayout.FIXED);
        btn.addListener(this);
        add(side, BorderLayout.EAST);

        add(_status = new BTextField(), BorderLayout.SOUTH);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("back".equals(event.getAction())) {

        } else if ("recruit".equals(event.getAction())) {
            UnitConfig config = _inspector.getConfig();
            // TODO: disable the button when this is not the case
            if (config != null && _inspector.getItemId() == -1 &&
                config.rank == UnitConfig.Rank.BIGSHOT) {
                recruit(config);
            }
        }
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

    protected void recruit (UnitConfig config)
    {
        RanchService rsvc = (RanchService)
            _ctx.getClient().requireService(RanchService.class);
        RanchService.ResultListener rl = new RanchService.ResultListener() {
            public void requestProcessed (Object result) {
                log.info("Recruited " + result);
            }
            public void requestFailed (String cause) {
                _status.setText(_msgs.xlate(cause));
            }
        };
        rsvc.recruitBigShot(_ctx.getClient(), config.type, rl);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected BTabbedPane _tabs;
    protected UnitPalette _bigshots, _units, _recruits;
    protected UnitInspector _inspector;
    protected BButton _uaction;
    protected BTextField _status;
}
