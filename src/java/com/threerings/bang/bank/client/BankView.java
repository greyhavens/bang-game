//
// $Id$

package com.threerings.bang.bank.client;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.util.Rectangle;
import com.jmex.bui.util.Point;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.coin.data.CoinExOfferInfo;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.bank.data.BankObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the main interface for the Bank.
 */
public class BankView extends ShopView
    implements BankCodes
{
    public BankView (BangContext ctx)
    {
        super(ctx, "bank");

        _status = new BTextArea();
        _status.setText(_ctx.xlate(BANK_MSGS, "m.welcome"));
        _status.setStyleClass("dialog_title");
        add(_status, new Rectangle(400, 25, 200, 5));

        add(_qsell = new QuickTransact(ctx, _status, false),
            new Rectangle(300, 30, 200, 500));
        add(_qbuy = new QuickTransact(ctx, _status, true),
            new Rectangle(300, 30, 525, 500));

        add(_fsell = new FullTransact(ctx, _status, false),
            new Rectangle(300, 500, 525, 100));
        add(_fbuy = new FullTransact(ctx, _status, true),
            new Rectangle(300, 500, 525, 500));

        add(new WalletLabel(ctx, false), new Rectangle(40, 78, 150, 40));
        add(new TownButton(ctx), new Point(650, 15));
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        BankObject bankobj = (BankObject)plobj;
        _qbuy.init(bankobj);
        _qsell.init(bankobj);
        _fbuy.init(bankobj);
        _fsell.init(bankobj);

        // issue a request for our outstanding offers
        BankService.OfferListener ol = new BankService.OfferListener() {
            public void gotOffers (CoinExOfferInfo[] buys,
                                   CoinExOfferInfo[] sells) {
                _fbuy.notePostedOffers(buys);
                _fsell.notePostedOffers(sells);
            }
            public void requestFailed (String reason) {
                log.warning("Failed to fetch our posted offers " +
                            "[reason=" + reason + "].");
            }
        };
        bankobj.service.getMyOffers(_ctx.getClient(), ol);
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    protected BTextArea _status;
    protected QuickTransact _qbuy, _qsell;
    protected FullTransact _fbuy, _fsell;
}
