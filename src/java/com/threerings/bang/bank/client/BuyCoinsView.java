//
// $Id$

package com.threerings.bang.bank.client;

import java.net.URL;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ResultListener;
import com.threerings.util.BrowserUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.bank.data.BankCodes;

/**
 * Displays a UI with information on coin packages available for purchase.
 */
public class BuyCoinsView extends BContainer
{
    public BuyCoinsView (BangContext ctx, StatusLabel status)
    {
        super(GroupLayout.makeHStretch());
        ((GroupLayout)getLayoutManager()).setGap(40);

        _ctx = ctx;
        _status = status;

        MessageBundle msgs =
            ctx.getMessageManager().getBundle(BankCodes.BANK_MSGS);

        BContainer lcont = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.LEFT));
        lcont.setPreferredSize(new Dimension(310, 50));
        lcont.add(new BLabel(msgs.get("m.great_offers"), "bank_title"));
        add(lcont, GroupLayout.FIXED);

        TableLayout tlay = new TableLayout(3, 5, 15);
        tlay.setHorizontalAlignment(TableLayout.CENTER);
        BContainer ocont = new BContainer(tlay);
        for (int ii = 0; ii < PACKAGES.length; ii++) {
            BLabel coins = new BLabel(String.valueOf(PACKAGES[ii]));
            coins.setIcon(BangUI.coinIcon);
            ocont.add(coins);
            ocont.add(new BLabel(msgs.get("m.for")));
            ocont.add(new BLabel(msgs.get("m.cost_" + PACKAGES[ii]),
                                 "offer_right"));
        }
        add(ocont);

        BContainer bcont = new BContainer(
            GroupLayout.makeVert(GroupLayout.CENTER));
        BButton buy = new BButton(msgs.get("m.purchase"));
        buy.setStyleClass("huge_button");
        bcont.add(buy);
        buy.addListener(_purchaser);
        add(bcont, GroupLayout.FIXED);
    }

    protected ActionListener _purchaser = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            BrowserUtil.browseURL(
                _shownURL = DeploymentConfig.getBillingURL(_ctx), _browlist);
        }
    };

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
    protected URL _shownURL;

    protected static final int[] PACKAGES = { 12, 42, 90 };
}
