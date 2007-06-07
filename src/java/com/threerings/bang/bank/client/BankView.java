//
// $Id$

package com.threerings.bang.bank.client;

import java.net.URL;

import com.jme.renderer.ColorRGBA;
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
import com.jmex.bui.util.Rectangle;
import com.jmex.bui.util.Point;

import com.samskivert.util.ResultListener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.coin.data.CoinExOfferInfo;

import com.threerings.util.BrowserUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.bank.data.BankObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the main interface for the Bank.
 */
public class BankView extends ShopView
    implements BankCodes, ActionListener
{
    public BankView (BangContext ctx)
    {
        super(ctx, BANK_MSGS);

        _ctx = ctx;

        add(_contents = new BContainer(new AbsoluteLayout()), new Rectangle(160, 60, 850, 594));

        add(new BLabel(_msgs.get("m.welcome"), "shop_status"), new Rectangle(200, 655, 625, 40));

        PlayerObject user = _ctx.getUserObject();
        String townId = user.townId;
        add(new BLabel(_msgs.get("m.name_" + townId), "shopkeep_name_label"),
            new Rectangle(12, 513, 155, 25));

        _status = new StatusLabel(ctx);
        _status.setStyleClass("shop_status");
        add(_status, new Rectangle(265, 10, 500, 40));
        _status.setText(getShopTip());

        add(new WalletLabel(ctx, true), new Rectangle(25, 40, 150, 40));
        add(createHelpButton(), new Point(790, 20));
        add(new TownButton(ctx), new Point(880, 20));

        if (user.canExchange() && user.holdsGoldPass(townId)) {
            showExchangeView();
        } else {
            showPassView();
        }
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _bankobj = (BankObject)plobj;

        initBank();
    }

    public void initBank ()
    {
        if (_qbuy != null && _bankobj != null) {
            _qbuy.init(_bankobj);
            _qsell.init(_bankobj);
            _fbuy.init(_bankobj);
            _fsell.init(_bankobj);

            updateMyOffers(_bankobj, false);
        }
    }

    /**
     * Update the list of the player's buy and sell offers.
     */
    public void updateMyOffers (BankObject bankobj, final boolean clear)
    {
        // issue a request for our outstanding offers
        BankService.OfferListener ol = new BankService.OfferListener() {
            public void gotOffers (CoinExOfferInfo[] buys, CoinExOfferInfo[] sells) {
                if (clear) {
                    _fbuy.clearPostedOffers();
                    _fsell.clearPostedOffers();
                }
                _fbuy.notePostedOffers(buys);
                _fsell.notePostedOffers(sells);
            }
            public void requestFailed (String reason) {
                log.warning("Failed to fetch our posted offers " + "[reason=" + reason + "].");
            }
        };
        bankobj.service.getMyOffers(_ctx.getClient(), ol);
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("get_pass".equals(event.getAction())) {
            BrowserUtil.browseURL(_shownURL = DeploymentConfig.getBillingPassURL(
                        _ctx, _ctx.getUserObject().townId), _browlist);
        } else if ("exchange".equals(event.getAction())) {
            if (_ctx.getUserObject().canExchange()) {
                showExchangeView();
            } else {
                _ctx.getBangClient().displayPopup(new ExchangeInfoWindow(), true, 500);
            }
        }
    }


    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    protected void showPassView ()
    {
        PlayerObject user = _ctx.getUserObject();
        String townId = user.townId;

        _contents.add(new BLabel(new BlankIcon(800, 24), "bank_divider"), new Point(30, 534));
        _contents.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/bank/heading_gold_pass.png"))),
                    new Point(60, 544));

        _contents.add(new BLabel(new ImageIcon(_ctx.loadImage(
                            "ui/bank/" + townId + "/splash.png"))), new Point(59, 186));
        _contents.add(new BLabel(new ImageIcon(_ctx.loadImage(
                            "ui/bank/" + townId + "/never_need.png"))), new Point(470, 348));
        BButton pass = new BButton(_msgs.get("m.get_pass"), this, "get_pass");
        pass.setStyleClass("massive_button");
        if (user.holdsGoldPass(townId)) {
            pass.setEnabled(false);
        }
        _contents.add(pass, new Point(507, 233));
        BContainer cost = new BContainer(GroupLayout.makeHoriz(
                    GroupLayout.CENTER).setOffAxisJustification(GroupLayout.BOTTOM).setGap(10));
        cost.add(new BLabel(_msgs.get("m.only"), "bank_only"));
        cost.add(new BLabel(_msgs.get("m.cost_pass"), "pass_cost"));
        _contents.add(cost, new Rectangle(507, 183, 285, 50));


        _contents.add(new BLabel(new BlankIcon(800, 24), "bank_divider"), new Point(30, 118));
        _contents.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/bank/heading_gold_coins.png"))),
                    new Point(60, 128));

        String msg = user.canExchange() ? "m.exchange_offers" : "m.great_offers";
        _contents.add(new BLabel(_msgs.get(msg), "bank_title"),
                    new Rectangle(80, 65, 250, 50));
        _contents.add(new BButton(_msgs.get("m.exchange"), this, "exchange"), new Point(115, 25));

        _contents.add(new BuyCoinsView(_ctx, _status), new Rectangle(375, 20, 375, 86));
    }

    protected void showExchangeView ()
    {
        _contents.removeAll();

        _contents.add(new BLabel(new BlankIcon(350, 24), "bank_divider"), new Point(30, 534));
        _contents.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/bank/heading_gold_coins.png"))),
                    new Point(60, 544));
        _contents.add(new BLabel(_msgs.get("m.great_offers"), "bank_title"),
                    new Rectangle(80, 484, 250, 50));

        _contents.add(new BuyCoinsView(_ctx, _status), new Rectangle(425, 480, 375, 86));

        _contents.add(new BLabel(new BlankIcon(800, 24), "bank_divider"), new Point(30, 435));
        _contents.add(new BLabel(new ImageIcon(_ctx.loadImage(
                            "ui/bank/heading_immediate_trades.png"))), new Point(60, 445));

        _contents.add(_qsell = new QuickTransact(_ctx, _status, false),
            new Rectangle(65, 403, 320, 30));
        _contents.add(_qbuy = new QuickTransact(_ctx, _status, true),
            new Rectangle(470, 403, 320, 30));

        _contents.add(new BLabel(new BlankIcon(800, 24), "bank_divider"), new Point(30, 345));
        _contents.add(new BLabel(new ImageIcon(_ctx.loadImage(
                            "ui/bank/heading_market_offers.png"))), new Point(60, 355));

        _contents.add(_fsell = new FullTransact(_ctx, this, _status, false),
            new Rectangle(65, 25, 330, 315));
        _contents.add(_fbuy = new FullTransact(_ctx, this, _status, true),
            new Rectangle(470, 25, 330, 315));
        initBank();
    }

    protected class ExchangeInfoWindow extends BDecoratedWindow
        implements ActionListener
    {
        public ExchangeInfoWindow ()
        {
            super(_ctx.getStyleSheet(), _msgs.get("m.einfo_title"));
            setModal(true);
            ((GroupLayout)getLayoutManager()).setGap(20);
            BContainer cont = new BContainer(GroupLayout.makeHoriz(GroupLayout.LEFT).setGap(10));

            cont.add(new BLabel(new ImageIcon(_ctx.loadImage("goods/passes/exchange.png"))));
            BLabel info = new BLabel(_msgs.get("m.einfo_info"));
            info.setPreferredSize(320, -1);
            cont.add(info);
            add(cont);

            BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
            ((GroupLayout)bcont.getLayoutManager()).setGap(25);
            bcont.add(new BButton(_msgs.get("m.to_store"), this, "to_store"));
            bcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
            add(bcont, GroupLayout.FIXED);
        }

        // from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            _ctx.getBangClient().clearPopup(this, true);
            if ("to_store".equals(event.getAction())) {
                BangBootstrapData bbd = (BangBootstrapData)_ctx.getClient().getBootstrapData();
                _ctx.getLocationDirector().moveTo(bbd.storeOid);
            }
        }
    }

    protected ResultListener _browlist = new ResultListener() {
        public void requestCompleted (Object result) {
        }
        public void requestFailed (Exception cause) {
            String msg = MessageBundle.tcompose(
                    "m.browser_launch_failed", _shownURL.toString());
            _status.setStatus(_ctx.xlate(BangAuthCodes.AUTH_MSGS, msg), true);
        }
    };

    protected BangContext _ctx;
    protected StatusLabel _status;
    protected QuickTransact _qbuy, _qsell;
    protected FullTransact _fbuy, _fsell;
    protected BContainer _contents;
    protected BankObject _bankobj;
    protected URL _shownURL;
}
