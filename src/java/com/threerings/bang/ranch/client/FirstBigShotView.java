//
// $Id$

package com.threerings.bang.ranch.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.PlayerService;
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
    implements ActionListener, IconPalette.Inspector
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

        UnitConfig[] units  = new UnitConfig[RanchCodes.STARTER_BIGSHOTS.length];
        for (int ii = 0; ii < units.length; ii++) {
            units[ii] = UnitConfig.getConfig(RanchCodes.STARTER_BIGSHOTS[ii]);
        }
        _bigshots = new UnitPalette(ctx, this, units.length);
        _bigshots.setUnits(units);
        add(_bigshots, GroupLayout.FIXED);

        add(_status);

        BTextArea name = new BTextArea(_msgs.get("m.firstbs_name"));
        add(name, GroupLayout.FIXED);

        BContainer ncont = GroupLayout.makeHBox(GroupLayout.CENTER);
        add(ncont, GroupLayout.FIXED);

        // TODO: add a validator that limits the name length and calls
        // _done.setEnabled() as appropriate
        ncont.add(_name = new BTextField(""));
        _name.setPreferredWidth(250);
        ncont.add(new Spacer(25, 0));

        ncont.add(_done = new BButton(_msgs.get("m.done"), this, "done"));
        _done.setEnabled(false);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.equals("done")) {
            pickBigShot();
        }
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconSelected (SelectableIcon icon)
    {
        _config = ((UnitIcon)icon).getUnit();
        String info = _umsgs.xlate(_config.getName()) + "\n";
        info += _umsgs.xlate(_config.getName() + "_descrip") + "\n";
        info += _umsgs.get("m.move_range", "" + _config.moveDistance) + "\n";
        info += _umsgs.get("m.fire_range", _config.getDisplayFireDistance());
        _status.setText(info);
        _done.setEnabled(isReady());
    }

    // documentation inherited from interface IconPalette.Inspector
    public void selectionCleared ()
    {
        _status.setText(_msgs.get("m.firstbs_tip"));
        _config = null;
        _done.setEnabled(isReady());
    }

    protected void pickBigShot ()
    {
        PlayerService psvc = (PlayerService)
            _ctx.getClient().requireService(PlayerService.class);
        PlayerService.ConfirmListener cl = new PlayerService.ConfirmListener() {
            public void requestProcessed () {
                dismiss();
                // move to the next phase of the intro
                _ctx.getBangClient().checkShowIntro();
            }
            public void requestFailed (String reason) {
                _status.setText(_msgs.xlate(reason));
                _done.setEnabled(true);
            }
        };
        _done.setEnabled(false);
        psvc.pickFirstBigShot(_ctx.getClient(), _config.type, cl);
    }

    protected boolean isReady ()
    {
        return (_config != null) /* && !StringUtil.isBlank(_name.getText()) */;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs, _umsgs;
    protected UnitConfig _config;

    protected UnitPalette _bigshots;
    protected BTextArea _status;
    protected BTextField _name;
    protected BButton _done;
}
