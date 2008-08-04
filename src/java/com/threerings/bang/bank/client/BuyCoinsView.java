//
// $Id$

package com.threerings.bang.bank.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.StatusLabel;
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

        MessageBundle msgs = ctx.getMessageManager().getBundle(BankCodes.BANK_MSGS);

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
            _ctx.showURL(DeploymentConfig.getBillingURL(_ctx));
        }
    };

    protected BangContext _ctx;
    protected StatusLabel _status;

    protected static final int[] PACKAGES = { 12, 42, 90 };
}
