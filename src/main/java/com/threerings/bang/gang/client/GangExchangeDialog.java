//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.ConsolidatedOffer;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.bank.client.QuickTransact;

import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;

/**
 * A Dialog to allow gang leaders to exchange gang scrip for gold.
 */
public class GangExchangeDialog extends BDecoratedWindow
    implements AttributeChangeListener, ActionListener
{
    public GangExchangeDialog (BangContext ctx, HideoutObject hobj, GangObject gobj)
    {
        super(ctx.getStyleSheet(), ctx.xlate(HideoutCodes.HIDEOUT_MSGS, "t.exchange_dialog"));
        setModal(true);
        _ctx = ctx;
        _hobj = hobj;
        _gobj = gobj;
        _msgs = ctx.getMessageManager().getBundle(HideoutCodes.HIDEOUT_MSGS);

        BContainer cont = new BContainer(GroupLayout.makeVert(GroupLayout.TOP).setGap(10));
        BContainer tradecont = new BContainer(new AbsoluteLayout());
        tradecont.setStyleClass("palette_border");
        tradecont.setPreferredSize(450, 160);
        tradecont.add(new BLabel(new BlankIcon(400, 24), "bank_divider"), new Point(15, 100));
        tradecont.add(new BLabel(new ImageIcon(_ctx.loadImage(
                            "ui/bank/heading_immediate_trades.png"))), new Point(45, 110));
        _status = new StatusLabel(_ctx) {
            public void setStatus (String message, boolean flash) {
                super.setStatus(message, flash);
                pack();
            }
        };
        tradecont.add(_buyer = new QuickTransact(_ctx, _status, true, gobj),
                new Rectangle(50, 50, 320, 35));
        BContainer currentPrice = new BContainer(
                GroupLayout.makeHoriz(GroupLayout.CENTER).setGap(5));
        currentPrice.add(new BLabel(_msgs.get("m.best_offer"), "match_label"));
        currentPrice.add(new BLabel(BangUI.coinIcon));
        currentPrice.add(_coinCost = new BLabel("0"));
        currentPrice.add(new BLabel(_msgs.get("m.for")));
        currentPrice.add(new BLabel(BangUI.scripIcon));
        currentPrice.add(_scripCost = new BLabel("0"));
        currentPrice.add(new BLabel(_msgs.get("m.each")));
        tradecont.add(currentPrice, new Rectangle(60, 5, 300, 40));
        cont.add(tradecont);
        _status.setPreferredSize(450, -1);
        cont.add(_status);
        BContainer gangScrip = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER).setGap(5));
        gangScrip.add(new BLabel(_msgs.get("m.coffers")));
        gangScrip.add(new BLabel(BangUI.scripIcon));
        gangScrip.add(_scrip = new BLabel("" + gobj.scrip));
        gangScrip.add(new BLabel(BangUI.coinIcon));
        gangScrip.add(_coins = new BLabel("" + gobj.coins));
        cont.add(gangScrip);
        cont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(cont);
    }

    // documentation inherited from ActionListener
    public void actionPerformed (ActionEvent event)
    {
        _ctx.getBangClient().clearPopup(this, true);
    }

    // documentation inherited from AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (HideoutObject.SELL_OFFERS.equals(event.getName())) {
            updateBest();
        } else if (GangObject.SCRIP.equals(event.getName())) {
            _scrip.setText("" + _gobj.scrip);
        } else if (GangObject.COINS.equals(event.getName())) {
            _coins.setText("" + _gobj.coins);
        }
    }

    protected void updateBest ()
    {
        ConsolidatedOffer best = _hobj.getBestSell();
        if (best == null) {
            _coinCost.setText("0");
            _scripCost.setText("0");
        } else {
            _coinCost.setText("" + best.volume);
            _scripCost.setText("" + best.price);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        _buyer.init(_hobj);
        _hobj.addListener(this);
        _gobj.addListener(this);

        updateBest();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        _hobj.removeListener(this);
        _gobj.removeListener(this);
    }

    protected BangContext _ctx;
    protected HideoutObject _hobj;
    protected GangObject _gobj;
    protected MessageBundle _msgs;

    protected StatusLabel _status;
    protected QuickTransact _buyer;
    protected BLabel _coinCost, _scripCost, _scri, _scrip, _coins;
}
