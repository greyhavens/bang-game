//
// $Id$

package com.threerings.bang.bank.client;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BLabel;
import com.jmex.bui.util.Rectangle;
import com.jmex.bui.util.Point;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.coin.data.CoinExOfferInfo;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
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
        super(ctx, BANK_MSGS);

        add(new BLabel(_msgs.get("m.welcome"), "shop_status"),
            new Rectangle(200, 655, 625, 40));

        String townId = _ctx.getUserObject().townId;
        add(new BLabel(_msgs.get("m.name_" + townId), "shopkeep_name_label"),
            new Rectangle(12, 513, 155, 25));

        _status = new StatusLabel(ctx);
        _status.setStyleClass("shop_status");
        add(_status, new Rectangle(265, 10, 500, 40));
        _status.setText(getShopTip());

        add(new BuyCoinsView(ctx, _status), new Rectangle(225, 540, 725, 86));

        add(_qsell = new QuickTransact(ctx, _status, false),
            new Rectangle(225, 463, 320, 30));
        add(_qbuy = new QuickTransact(ctx, _status, true),
            new Rectangle(630, 463, 320, 30));

        add(_fsell = new FullTransact(ctx, _status, false),
            new Rectangle(225, 85, 330, 315));
        add(_fbuy = new FullTransact(ctx, _status, true),
            new Rectangle(630, 85, 330, 315));

        add(new WalletLabel(ctx, true), new Rectangle(25, 40, 150, 40));
        add(createHelpButton(), new Point(790, 20));
        add(new TownButton(ctx), new Point(880, 20));
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

    protected StatusLabel _status;
    protected QuickTransact _qbuy, _qsell;
    protected FullTransact _fbuy, _fsell;
}
