//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
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

        // TODO: add avatar and info views
        add(new BContainer(), BorderLayout.CENTER);

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

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected ParlorObject _parobj;
    protected BButton _action;
}
