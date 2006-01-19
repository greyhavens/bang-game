//
// $Id$

package com.threerings.bang.bank.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

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
    public QuickTransact (BangContext ctx, BLabel status, boolean buying)
    {
        super(GroupLayout.makeHStretch());
        _ctx = ctx;
        _status = status;
        _buying = buying;
        _msgs = ctx.getMessageManager().getBundle(BANK_MSGS);

        String msg = buying ? "m.buy" : "m.sell";
        add(new BLabel(_msgs.get(msg)), GroupLayout.FIXED);
        add(new BLabel(BangUI.coinIcon), GroupLayout.FIXED);
        add(_coins = new BTextField(), GroupLayout.FIXED);
        _coins.setPreferredWidth(30);
        _coins.addListener(_coinlist);
        add(new BLabel(_msgs.get("m.for")), GroupLayout.FIXED);
        add(_scrip = new BLabel(BangUI.scripIcon));
        _scrip.setIconTextGap(5);
        add(_trade = new BButton(_msgs.get(msg), this, "go"), GroupLayout.FIXED);
        _trade.setEnabled(false);
    }

    public void init (BankObject bankobj)
    {
        _bankobj = bankobj;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // this should never happen but better safe than sorry
        if (_ccount == -1) {
            return;
        }

        // determine the best offer price
        ConsolidatedOffer best = _buying ?
            _bankobj.getBestSell() : _bankobj.getBestBuy();
        if (best == null) {
            return;
        }

        BankService.ResultListener rl = new BankService.ResultListener() {
            public void requestProcessed (Object result) {
                _status.setText(_msgs.get("m.trans_completed"));
                _coins.setText("");
                _scrip.setText("0");
            }
            public void requestFailed (String reason) {
                _status.setText(_msgs.xlate(reason));
            }
        };

        _bankobj.service.postOffer(
            _ctx.getClient(), _ccount, best.price, _buying, true, rl);
    }

    protected void coinsUpdated ()
    {
        // clear out the trade and only enable it if all is well
        clearTrade();

        if (StringUtil.isBlank(_coins.getText())) {
            _status.setText("");
            return;
        }

        try {
            _ccount = Integer.parseInt(_coins.getText());

            // make sure we have a best offer
            ConsolidatedOffer best = _buying ?
                _bankobj.getBestSell() : _bankobj.getBestBuy();
            if (best == null) {
                _status.setText(_msgs.get("m.no_offers"));
                return;
            }

            if (_ccount > best.volume) {
                String msg = MessageBundle.tcompose(
                    "m.exceeds_best_offer", _msgs.get("m.coins", best.volume));
                _status.setText(_msgs.xlate(msg));
                return;
            }

            _scrip.setText(String.valueOf(best.price * _ccount));
            _trade.setEnabled(true);
            _status.setText("");

        } catch (Exception e) {
            _status.setText(_msgs.get("m.invalid_coins"));
        }
    }

    protected void clearTrade ()
    {
        _scrip.setText("");
        _trade.setEnabled(false);
        _ccount = -1;
    }

    protected TextListener _coinlist = new TextListener() {
        public void textChanged (TextEvent event) {
            coinsUpdated();
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BankObject _bankobj;
    protected boolean _buying;
    protected int _ccount;

    protected BLabel _status;
    protected BTextField _coins;
    protected BLabel _scrip;
    protected BButton _trade;
}
