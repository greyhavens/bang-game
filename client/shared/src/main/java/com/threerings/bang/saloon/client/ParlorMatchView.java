//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BComponent;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.TableGameObject;

/**
 * Displays a pending match in a Back Parlor.
 */
public class ParlorMatchView extends BContainer
    implements ActionListener
{
    public ParlorMatchView (BangContext ctx, TableGameObject tobj)
    {
        super(new BorderLayout(0, 10));

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);
        _tobj = tobj;

        // this will contain the players and game info
        BContainer main = new BContainer(GroupLayout.makeHStretch());
        main.add(_left = GroupLayout.makeVBox(GroupLayout.CENTER));
        ((GroupLayout)_left.getLayoutManager()).setGap(0);
        main.add(_info = GroupLayout.makeVBox(GroupLayout.CENTER),
                 GroupLayout.FIXED);
        main.add(_right = GroupLayout.makeVBox(GroupLayout.CENTER));
        ((GroupLayout)_right.getLayoutManager()).setGap(0);
        main.setPreferredSize(new Dimension(395, 203));
        add(main, BorderLayout.CENTER);

        // create our player slots
        _slots = new PlayerSlot[_tobj.playerOids.length];
        boolean team = _tobj.game.mode == ParlorGameConfig.Mode.TEAM_2V2;
        if (team) {
            _joins = new BButton[_tobj.playerOids.length];
        }
        int idx = 0, pidx = 0;
        for (ParlorGameConfig.Slot slot : _tobj.game.slots) {
            int color = team ? TEAM_COLORS[idx] : idx + 1;
            BComponent comp = null;
            switch (slot) {
            case HUMAN:
                _slots[pidx] = new PlayerSlot(_ctx, color);
                if (team) {
                    BContainer cont = new BContainer(new AbsoluteLayout());
                    cont.add(_slots[pidx], new Point(0, 0));
                    _joins[pidx] = new BButton(_msgs.get("m.join"), this, "join_" + pidx);
                    _joins[pidx].setStyleClass("alt_button");
                    cont.add(_joins[pidx], new Rectangle(0, 0, 120, 23));
                    comp = cont;
                } else {
                    comp = _slots[pidx];
                }
                idx++;
                pidx++;
                break;
            case TINCAN:
                BContainer cont =
                    new BContainer(GroupLayout.makeVert(GroupLayout.CENTER).setGap(-3));
                cont.add(new Spacer(96, 20));
                cont.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/saloon/tin_can.png"))));
                cont.add(new BLabel(_msgs.get("m.tin_can"), "tin_can_slot" + color));
                comp = cont;
                idx++;
                break;
            default:
                continue;
            }
            if ((team && idx <= 2) || (!team && idx % 2 == 1)) {
                _left.add(comp);
            } else {
                _right.add(comp);
            }
        }

        // this will contain our game configuration
        _info.add(_mode = new BLabel("", "match_label"));
        _info.add(_rounds = new BLabel("", "match_label"));
        _info.add(_teams = new BLabel("", "match_label"));
        _info.add(_duration = new BLabel("", "match_label"));
        _info.add(_speed = new BLabel("", "match_label"));
        _info.add(_scenarios = new BLabel("", "match_label"));
        _info.add(_starting = new BLabel("", "starting_label"));

        _buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        String action = isJoined() ? "leave" : "join";
        _action = new BButton(_msgs.get("m." + action), this, action);
        _action.setStyleClass("big_button");
        if (team) {
            if (isJoined()) {
                for (int ii = 0; ii < _tobj.playerOids.length; ii++) {
                    _joins[ii].setVisible(false);
                }
                _buttons.add(_action);
            } else {
                for (int ii = 0; ii < _tobj.playerOids.length; ii++) {
                    if (_tobj.playerOids[ii] != 0) {
                        _joins[ii].setVisible(false);
                    }
                }
                _buttons.add(_joinBtn = new BLabel(new ImageIcon(
                                _ctx.loadImage("ui/saloon/click_a_join_button.png"))));
            }
        } else {
            _buttons.add(_action);
        }
        add(_buttons, BorderLayout.SOUTH);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if ("join".equals(action)) {
            _tobj.service.joinMatch();

        } else if (action.startsWith("join_")) {
            try {
                int slot = Integer.parseInt(action.substring(5));
                _tobj.service.joinMatchSlot(slot);
            } catch (NumberFormatException nfe) {
                _tobj.service.joinMatch();
            }

        } else if ("leave".equals(action)) {
            _tobj.service.leaveMatch();
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // add our listeners
        _tobj.addListener(_elup);
        _tobj.addListener(_atch);

        // update our displays
        updateDisplay();
        updateCriterion();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // clear out our listeners
        _tobj.removeListener(_elup);
        _tobj.removeListener(_atch);
    }

    protected boolean isJoined ()
    {
        int oid = _ctx.getUserObject().getOid();
        for (int ii = 0; ii < _tobj.playerOids.length; ii++) {
            if (_tobj.playerOids[ii] == oid) {
                return true;
            }
        }
        return false;
    }

    protected void updateCriterion ()
    {
        _rounds.setText(_msgs.get("m.cr_rounds", "" + _tobj.game.rounds));
        _teams.setText(_msgs.get("m.cr_teamsize", "" + _tobj.game.teamSize));
        _mode.setText(_msgs.xlate(
            MessageBundle.compose("m.cr_mode", MODES[_tobj.game.mode.ordinal()])));
        _duration.setText(_msgs.xlate(
            MessageBundle.compose("m.cr_duration", _tobj.game.duration.key())));
        _speed.setText(_msgs.xlate(MessageBundle.compose("m.cr_speed", _tobj.game.speed.key())));
// TODO
//         _scenarios.setText(_msgs.get("m.cr_scenarios", "" + TODO));
    }

    protected void updateStarting ()
    {
        _starting.setText(_tobj.starting ? _msgs.get("m.starting") : "");
    }

    protected void updateDisplay ()
    {
        boolean visible = !isJoined();
        for (int ii = 0; ii < _tobj.playerOids.length; ii++) {
            _slots[ii].setPlayerOid(_tobj.playerOids[ii]);
            if (_joins != null) {
                _joins[ii].setVisible(visible && _tobj.playerOids[ii] == 0);
            }
        }
        if (visible && _joinBtn != null && !_joinBtn.isAdded()) {
            _buttons.removeAll();
            _buttons.add(_joinBtn);
        } else if (!visible && !_action.isAdded()) {
            _buttons.removeAll();
            _buttons.add(_action);
        }
    }

    protected AttributeChangeListener _atch = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(TableGameObject.STARTING)) {
                updateStarting();
            }
        }
    };

    protected ElementUpdateListener _elup = new ElementUpdateListener() {
        public void elementUpdated (ElementUpdatedEvent event) {
            updateDisplay();
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected TableGameObject _tobj;
    protected BButton _action;
    protected BContainer _buttons;

    protected BContainer _left, _right, _info;
    protected PlayerSlot[] _slots;
    protected BButton[] _joins;

    protected BLabel _rounds, _teams, _mode, _duration, _speed, _scenarios;
    protected BLabel _starting, _joinBtn;

    protected static final String[] MODES = { "m.mode_normal", "m.mode_2v2" };
    protected static final int[] TEAM_COLORS = { 1, 6, 2, 5 };
}
