//
// $Id$

package com.threerings.bang.bank.client;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BWindow;
import com.jmex.bui.Spacer;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.coin.data.CoinExOfferInfo;

import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.bank.data.BankObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the main interface for the Bank.
 */
public class BankView extends BWindow
    implements PlaceView, BankCodes
{
    public BankView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), GroupLayout.makeHStretch());
        setStyleClass("main_view");

        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);

        String townId = _ctx.getUserObject().townId;

        // the left column contains some fancy graphics
        BContainer left = new BContainer(GroupLayout.makeVert(GroupLayout.TOP));
        add(left, GroupLayout.FIXED);
        String path = "ui/" + townId + "/bankteller.png";
        left.add(new BLabel(new ImageIcon(_ctx.loadImage(path))));

        // in the main area we have the main thing
        BContainer main = new BContainer(GroupLayout.makeVStretch());
        add(main);

        _status = new BTextArea();
        _status.setPreferredSize(new Dimension(100, 100));
        _status.setText(_ctx.xlate(BANK_MSGS, "m.welcome"));
        _status.setStyleClass("dialog_title");
        main.add(_status, GroupLayout.FIXED);

        String title = _ctx.xlate(BANK_MSGS, "m.quick_title");
        main.add(wrap(title, _qsell = new QuickTransact(ctx, _status, false),
                      _qbuy = new QuickTransact(ctx, _status, true)),
                 GroupLayout.FIXED);

        main.add(new Spacer(10, 10), GroupLayout.FIXED);

        title = _ctx.xlate(BANK_MSGS, "m.full_title");
        main.add(wrap(title, _fsell = new FullTransact(ctx, _status, false),
                      _fbuy = new FullTransact(ctx, _status, true)));

        // add a row displaying our cash on hand and the back button
        BContainer bottom = new BContainer(GroupLayout.makeHStretch());
        main.add(bottom, GroupLayout.FIXED);

        bottom.add(new WalletLabel(ctx, false));
        bottom.add(new TownButton(ctx), GroupLayout.FIXED);
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

    protected BContainer wrap (String title, BContainer left, BContainer right)
    {
        BContainer wrapper = new BContainer(new BorderLayout(5, 5));
        wrapper.add(new BLabel(title, "dialog_title"), BorderLayout.NORTH);
        BContainer pair = new BContainer(GroupLayout.makeHStretch());
        pair.add(left);
        pair.add(right);
        wrapper.add(pair, BorderLayout.CENTER);
        return wrapper;
    }

    protected BangContext _ctx;
    protected BTextArea _status;
    protected QuickTransact _qbuy, _qsell;
    protected FullTransact _fbuy, _fsell;
}
