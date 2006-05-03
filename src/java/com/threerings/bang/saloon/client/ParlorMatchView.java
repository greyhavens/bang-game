//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonCodes;

/**
 * Displays a pending match in a Back Parlor.
 */
public class ParlorMatchView extends BContainer
    implements ActionListener
{
    public ParlorMatchView (BangContext ctx, ParlorObject parobj)
    {
        super(new BorderLayout(0, 10));

        _ctx = ctx;
        _parobj = parobj;
        _msgs = ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);

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
        _slots = new PlayerSlot[_parobj.game.players];
        for (int ii = 0; ii < _slots.length; ii++) {
            if (ii % 2 == 0) {
                _left.add(_slots[ii] = new PlayerSlot(_ctx));
            } else {
                _right.add(_slots[ii] = new PlayerSlot(_ctx));
            }
        }
        updateDisplay();

        // this will contain our current criterion
        _info.add(_rounds = new BLabel("", "match_label"));
        _info.add(_players = new BLabel("", "match_label"));
        _info.add(_ranked = new BLabel("", "match_label"));
        _info.add(_range = new BLabel("", "match_label"));
        _info.add(_opponents = new BLabel("", "match_label"));
        _info.add(_starting = new BLabel("", "starting_label"));

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        String action = isJoined() ? "leave" : "join";
        _action = new BButton(_msgs.get("m." + action), this, action);
        _action.setStyleClass("big_button");
        buttons.add(_action);
        add(buttons, BorderLayout.SOUTH);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("join".equals(event.getAction())) {
            _parobj.service.joinMatch(_ctx.getClient());
        } else if ("leave".equals(event.getAction())) {
            _parobj.service.leaveMatch(_ctx.getClient());
        }
    }

    protected boolean isJoined ()
    {
        int oid = _ctx.getUserObject().getOid();
        for (int ii = 0; ii < _parobj.playerOids.length; ii++) {
            if (_parobj.playerOids[ii] == oid) {
                return true;
            }
        }
        return false;
    }

    protected void updateDisplay ()
    {
        for (int ii = 0; ii < _parobj.playerOids.length; ii++) {
            _slots[ii].setPlayerOid(_parobj.playerOids[ii]);
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected ParlorObject _parobj;
    protected BButton _action;

    protected BContainer _left, _right, _info;
    protected PlayerSlot[] _slots;

    protected BLabel _players, _rounds, _ranked, _range, _opponents;
    protected BLabel _starting;
}
