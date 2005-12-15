//
// $Id$

package com.threerings.bang.ranch.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.ranch.data.RanchCodes;

/**
 * Displays an interface introducing the players to Big Shots and allowing them
 * to select one.
 */
public class FirstBigShotView extends BDecoratedWindow
    implements ActionListener
{
    public FirstBigShotView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), null);
        setLayoutManager(GroupLayout.makeVStretch());

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(RanchCodes.RANCH_MSGS);
        _umsgs = ctx.getMessageManager().getBundle(BangCodes.UNITS_MSGS);

        _status = new BTextArea(_msgs.get("m.firstbs_tip"));

        BLabel title = new BLabel(_msgs.get("m.firstbs_title"));
        title.setHorizontalAlignment(BLabel.CENTER);
        title.setLookAndFeel(BangUI.dtitleLNF);
        add(title, GroupLayout.FIXED);

        BTextArea intro = new BTextArea(_msgs.get("m.firstbs_intro"));
        add(intro, GroupLayout.FIXED);

        InfoDisplay info = new InfoDisplay();

        UnitConfig[] units  = new UnitConfig[RanchCodes.STARTER_BIGSHOTS.length];
        for (int ii = 0; ii < units.length; ii++) {
            units[ii] = UnitConfig.getConfig(RanchCodes.STARTER_BIGSHOTS[ii]);
        }
        _bigshots = new UnitPalette(ctx, info, units.length);
        _bigshots.setUnits(units);
        add(_bigshots, GroupLayout.FIXED);
        add(info, GroupLayout.FIXED);

        add(_status, GroupLayout.FIXED);

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(_done = new BButton(_msgs.get("m.done"), this, "done"));
        add(buttons, GroupLayout.FIXED);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.equals("random")) {

        } else if (cmd.equals("done")) {
            dismiss();
            _ctx.getBangClient().checkShowIntro();
        }
    }

    protected class InfoDisplay extends BContainer
        implements IconPalette.Inspector
    {
        public InfoDisplay () {
            super(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                       GroupLayout.STRETCH));
            add(_descrip = new BLabel(""));
            add(_move = new BLabel(""));
            add(_fire = new BLabel(""));
        }

        public void iconSelected (SelectableIcon icon) {
            UnitConfig config = ((UnitIcon)icon).getUnit();
            _descrip.setText(_umsgs.xlate(config.getName() + "_descrip"));
            _move.setText(_umsgs.get("m.move_range", "" + config.moveDistance));
            String fire;
            if (config.minFireDistance == config.maxFireDistance) {
                fire = "" + config.minFireDistance;
            } else {
                fire = config.minFireDistance + " - " + config.maxFireDistance;
            }
            _fire.setText(_umsgs.get("m.fire_range", fire));
        }

        public void selectionCleared () {
            _descrip.setText("");
            _move.setText("");
            _fire.setText("");
        }

        protected BLabel _descrip, _move, _fire;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs, _umsgs;

    protected UnitPalette _bigshots;
    protected BTextArea _status;
    protected BButton _done;
}
