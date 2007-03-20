//
// $Id$

package com.threerings.bang.ranch.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.bang.client.BangClient;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.ranch.data.RanchCodes;

/**
 * Displays an interface introducing the players to Big Shots and allowing them
 * to select one.
 */
public class FirstBigShotView extends SteelWindow
    implements ActionListener, IconPalette.Inspector,
               BangClient.NonClearablePopup
{
    public FirstBigShotView (BangContext ctx)
    {
        super(ctx, ctx.xlate(RanchCodes.RANCH_MSGS, "m.firstbs_title"));
        setModal(true);
        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(15));
        _contents.setStyleClass("padded");

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(RanchCodes.RANCH_MSGS);
        _umsgs = ctx.getMessageManager().getBundle(BangCodes.UNITS_MSGS);

        _status = new BLabel(_msgs.get("m.firstbs_tip"), "dialog_text");

        _contents.add(new BLabel(_msgs.get("m.firstbs_intro"), "dialog_text"));

        UnitConfig[] units = new UnitConfig[RanchCodes.STARTER_BIGSHOTS.length];
        for (int ii = 0; ii < units.length; ii++) {
            units[ii] = UnitConfig.getConfig(
                RanchCodes.STARTER_BIGSHOTS[ii], true);
        }
        _bigshots = new UnitPalette(ctx, this, units.length, 1);
        _bigshots.setShowNavigation(false);
        _bigshots.setPaintBorder(true);
        _bigshots.setUnits(units, false);
        _contents.add(_bigshots);

        BContainer ncont = GroupLayout.makeHBox(GroupLayout.CENTER);
        _contents.add(ncont, GroupLayout.FIXED);

        ncont.add(new BLabel(_msgs.get("m.firstbs_name"), "dialog_label"));
        ncont.add(_name = new BTextField(BigShotItem.MAX_NAME_LENGTH));
        _name.setPreferredWidth(150);
        _name.addListener(new TextListener() {
            public void textChanged (TextEvent event) {
                if (_name.getText().length() > BigShotItem.MAX_NAME_LENGTH) {
                    _status.setText(_msgs.get("m.name_to_long"));
                } else {
                    _status.setText(_msgs.get("m.firstbs_tip"));
                }
                _done.setEnabled(isReady());
            }
        });
        ncont.add(BangUI.createDiceButton(this, "random"));
        ncont.add(new Spacer(25, 0));

        _contents.add(_status);

        _buttons.add(_done = new BButton(_msgs.get("m.done"), this, "done"));
        _done.setEnabled(false);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.equals("random") && _config != null) {
            _name.setText(_config.pickRandomName());

        } else if (cmd.equals("done")) {
            pickBigShot();
        }
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        if (selected) {
            _config = ((UnitIcon)icon).getUnit();
            _name.setText(_config.pickRandomName());
            _status.setText(_umsgs.xlate(_config.getTip()));
        } else {
            _status.setText(_msgs.get("m.firstbs_tip"));
            _config = null;
        }
        _done.setEnabled(isReady());
    }

    protected void pickBigShot ()
    {
        PlayerService psvc = (PlayerService)
            _ctx.getClient().requireService(PlayerService.class);
        PlayerService.ConfirmListener cl = new PlayerService.ConfirmListener() {
            public void requestProcessed () {
                // move to the next phase of the intro
                _ctx.getBangClient().clearPopup(FirstBigShotView.this, true);
                _ctx.getBangClient().continueMoveTo();
            }
            public void requestFailed (String reason) {
                _status.setText(_msgs.xlate(reason));
                _done.setEnabled(true);
            }
        };
        _done.setEnabled(false);
        Name name = new Name(_name.getText());
        // TODO: any name validation? no one really sees this name
        psvc.pickFirstBigShot(_ctx.getClient(), _config.type, name, cl);
    }

    protected boolean isReady ()
    {
        return (_config != null) && _name.getText().length() > 0 &&
            _name.getText().length() <= BigShotItem.MAX_NAME_LENGTH;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs, _umsgs;
    protected UnitConfig _config;

    protected UnitPalette _bigshots;
    protected BLabel _status;
    protected BTextField _name;
    protected BButton _done;
}
