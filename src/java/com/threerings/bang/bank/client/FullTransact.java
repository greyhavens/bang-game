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
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.bank.data.BankObject;

/**
 * Displays an interface for posting a buy or sell offer and for viewing one's
 * outstanding offers.
 */
public class FullTransact extends BContainer
    implements ActionListener
{
    public FullTransact (BangContext ctx, boolean buying)
    {
        super(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                   GroupLayout.STRETCH));
        _ctx = ctx;
        _buying = buying;
        _msgs = ctx.getMessageManager().getBundle(BankCodes.BANK_MSGS);

        String msg = buying ? "m.buy" : "m.sell";
        add(new BLabel(_msgs.get(msg + "_offers")));

        // add slots for the top four offers
        BContainer offers = new BContainer(new TableLayout(4, 5, 2));
        _offers = new OfferLabel[4];
        for (int ii = 0; ii < _offers.length; ii++) {
            _offers[ii] = new OfferLabel(offers);
        }
        _offers[0].setNoOffers();
        add(offers);
        add(new Spacer(1, 15));

        add(new BLabel(_msgs.get(msg + "_post_offer")));

        BContainer moffer = GroupLayout.makeHBox(GroupLayout.LEFT);
        moffer.add(new BLabel(BangUI.coinIcon));
        moffer.add(_coins = new BTextField(""));
        _coins.setPreferredWidth(30);
        moffer.add(new BLabel(_msgs.get("m.for")));
        moffer.add(new BLabel(BangUI.scripIcon));
        moffer.add(_scrip = new BTextField(""));
        _scrip.setPreferredWidth(40);
        moffer.add(new BLabel(_msgs.get("m.each")));
        moffer.add(new Spacer(15, 1));
        moffer.add(new BButton(_msgs.get("m.post", this, "post")));
        add(moffer);
        add(new Spacer(1, 15));

        add(new BLabel(_msgs.get("m.your_offers")));
        BContainer myoffers = new BContainer(new TableLayout(5, 5, 2));
        add(myoffers);

        // TODO: extract our offers from the supplied set
        OfferLabel mine = new OfferLabel(myoffers);
        mine.setOffer(5, 1234);
        BButton rescind = new BButton(_msgs.get("m.rescind", this, "rescind"));
        // rescind.setProperty("offer", offer);
        myoffers.add(rescind);
    }

    public void init (BankObject bankobj)
    {
        _bankobj = bankobj;
        _offers[0].setOffer(5, 1253);
        _offers[1].setOffer(13, 1223);
        _offers[2].setOffer(25, 1103);
        _offers[3].setOffer(3, 1100);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("post".equals(event.getAction())) {
            // do the deed
        } else if ("rescind".equals(event.getAction())) {
            // do the deed
        }
    }

    protected class OfferLabel
    {
        public OfferLabel (BContainer table) {
            table.add(_clabel = new BLabel(""));
            table.add(_coins = new BLabel(""));
            _coins.setHorizontalAlignment(BLabel.RIGHT);
            table.add(_slabel = new BLabel(""));
            table.add(_scrip = new BLabel(""));
            _scrip.setHorizontalAlignment(BLabel.RIGHT);
        }

        public void setOffer (int coins, int scrip)
        {
            _clabel.setIcon(BangUI.coinIcon);
            _coins.setText(_msgs.get("m.offer_coins", "" + coins));
            _slabel.setIcon(BangUI.scripIcon);
            _scrip.setText(_msgs.get("m.offer_scrip", "" + scrip));
        }

        public void setNoOffers ()
        {
            clearOffer();
            _coins.setText(_msgs.get("m.no_offers"));
        }

        public void clearOffer ()
        {
            _clabel.setIcon(null);
            _coins.setText("");
            _slabel.setIcon(null);
            _scrip.setText("");
        }

        protected BLabel _clabel, _coins;
        protected BLabel _slabel, _scrip;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BankObject _bankobj;
    protected boolean _buying;

    protected OfferLabel[] _offers;
    protected BTextField _coins, _scrip;
}
