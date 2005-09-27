//
// $Id$

package com.threerings.bang.bank.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bank.data.BankObject;

/**
 * Displays an interface for making a quick transaction: an immediately
 * executed buy or sell order.
 */
public class QuickTransact extends BContainer
    implements ActionListener
{
    public QuickTransact (BangContext ctx, boolean buying)
    {
        super(GroupLayout.makeHoriz(GroupLayout.LEFT));
        _ctx = ctx;
        _buying = buying;

        String msg = buying ? "m.buy" : "m.sell";
        add(new BLabel(_ctx.xlate("bank", msg)));
        add(new BLabel(BangUI.coinIcon));
        add(_coins = new BTextField());
        _coins.setPreferredWidth(30);
        add(new BLabel(_ctx.xlate("bank", "m.for")));
        add(_scrip = new BLabel(BangUI.scripIcon));
        _scrip.setText("0");
        add(new Spacer(15, 1));
        add(new BButton(_ctx.xlate("bank", msg), this, "go"));
    }

    public void init (BankObject bankobj)
    {
        _bankobj = bankobj;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("go".equals(event.getAction())) {
            // do the deed
        }
    }

    protected BangContext _ctx;
    protected BankObject _bankobj;
    protected boolean _buying;
    protected BTextField _coins;
    protected BLabel _scrip;
}
