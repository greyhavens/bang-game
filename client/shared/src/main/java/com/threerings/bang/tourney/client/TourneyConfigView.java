//
// $Id$

package com.threerings.bang.tourney.client;

import com.samskivert.util.StringUtil;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.layout.GroupLayout;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;
import com.threerings.presents.client.InvocationService.ResultListener;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.tourney.data.BangTourneyCodes;
import com.threerings.bang.tourney.data.BangTourneyConfig;
import com.threerings.bang.client.bui.StatusLabel;

import com.threerings.parlor.tourney.client.TourniesService;

/**
 * Does something extraordinary.
 */
public class TourneyConfigView extends BDecoratedWindow
    implements ActionListener, BangTourneyCodes
{
    public TourneyConfigView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), ctx.xlate(BangCodes.TOURNEY_MSGS, "m.config_title"));

        setModal(true);

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(BangCodes.TOURNEY_MSGS);

        BContainer cont = new BContainer(new TableLayout(2, 10, 10));

        cont.add(new BLabel(_msgs.get("m.tourney_desc"), "right_label"));
        cont.add(_desc = new BTextField(30));
        _desc.setPreferredSize(150, -1);

        cont.add(new BLabel(_msgs.get("m.min_players"), "right_label"));
        cont.add(_minPlayers = new BComboBox());
        for (int ii : MIN_PLAYERS) {
            _minPlayers.addItem(new Integer(ii));
        }
        _minPlayers.selectItem(0);

        cont.add(new BLabel(_msgs.get("m.starts_in"), "right_label"));
        cont.add(_startsIn = new BComboBox());
        for (int ii : STARTS_IN) {
            _startsIn.addItem(new Integer(ii));
        }
        _startsIn.selectItem(0);

        add(cont);

        add(_statusLabel = new StatusLabel(_ctx), GroupLayout.FIXED);
        _statusLabel.setPreferredSize(400, 40);

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        bcont.add(_createBtn = new BButton(_msgs.get("m.create"), this, "create"));
        bcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if ("dismiss".equals(cmd)) {
            _ctx.getBangClient().clearPopup(this, true);

        } else if ("create".equals(cmd)) {
            createTourney();
        }
    }

    protected void createTourney ()
    {
        BangTourneyConfig config = new BangTourneyConfig();
        config.desc = _desc.getText();
        if (StringUtil.isBlank(config.desc)) {
            _statusLabel.setStatus(_msgs.get("m.no_desc"), true);
            return;
        }

        // disable the create button to prevent double-clicks
        _createBtn.setEnabled(false);
        _statusLabel.setStatus(_msgs.get("m.creating_tourney"), false);

        ResultListener rl = new ResultListener() {
            public void requestProcessed (Object result) {
                _ctx.getBangClient().clearPopup(TourneyConfigView.this, true);
            }
            public void requestFailed (String cause) {
                _statusLabel.setStatus(_msgs.get(cause), true);
                _createBtn.setEnabled(true);
            }
        };

        _ctx.getClient().requireService(TourniesService.class).createTourney(config, rl);
    }

    protected BangContext _ctx;

    protected MessageBundle _msgs;

    protected BComboBox _minPlayers;
    protected BComboBox _startsIn;
    protected BTextField _desc;
    protected StatusLabel _statusLabel;
    protected BButton _createBtn;
}
