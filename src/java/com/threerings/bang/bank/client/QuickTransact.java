//
// $Id$

package com.threerings.bang.bank.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.ConsolidatedOffer;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.bank.data.BankObject;

/**
 * Displays an interface for making a quick transaction: an immediately
 * executed buy or sell order.
 */
public class QuickTransact extends BContainer
    implements ActionListener, BankCodes
{
    public QuickTransact (BangContext ctx, BTextArea status, boolean buying)
    {
        super(GroupLayout.makeHoriz(GroupLayout.LEFT));
        _ctx = ctx;
        _status = status;
        _buying = buying;

        String msg = buying ? "m.buy" : "m.sell";
        add(new BLabel(_ctx.xlate(BANK_MSGS, msg)));
        add(new BLabel(BangUI.coinIcon));
        add(_coins = new BTextField());
        _coins.setPreferredWidth(30);
        add(new BLabel(_ctx.xlate(BANK_MSGS, "m.for")));
        add(_scrip = new BLabel(BangUI.scripIcon));
        _scrip.setText("0");
        add(new Spacer(15, 1));
        add(new BButton(_ctx.xlate(BANK_MSGS, msg), this, "go"));
    }

    public void init (BankObject bankobj)
    {
        _bankobj = bankobj;

        // TODO: add a listener that keeps _scrip up to date and disables our
        // action button if it is not possible to execute an immediate trade
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // determine the best offer price
        ConsolidatedOffer best = _buying ?
            _bankobj.getBestBuy() : _bankobj.getBestSell();
        if (best == null) {
            return;
        }

        BankService.ConfirmListener cl = new BankService.ConfirmListener() {
            public void requestProcessed () {
                _status.setText(_ctx.xlate(BANK_MSGS, "m.trans_completed"));
                _coins.setText("");
                _scrip.setText("0");
            }
            public void requestFailed (String reason) {
                _status.setText(_ctx.xlate(BANK_MSGS, reason));
            }
        };

        try {
            int coins = Integer.valueOf(_coins.getText());
            _bankobj.service.postOffer(_ctx.getClient(), coins, best.price,
                                       _buying, true, cl);
        } catch (Exception e) {
            // TODO: make BTextField support input restriction
            _status.setText(_ctx.xlate(BANK_MSGS, "m.invalid_coins"));
        }
    }

    protected BangContext _ctx;
    protected BankObject _bankobj;
    protected boolean _buying;

    protected BTextArea _status;
    protected BTextField _coins;
    protected BLabel _scrip;
}
