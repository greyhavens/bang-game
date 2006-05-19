//
// $Id$

package com.threerings.bang.bank.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;

/**
 * Displays a UI with information on coin packages available for purchase.
 */
public class BuyCoinsView extends BContainer
{
    public BuyCoinsView (BangContext ctx)
    {
        super(GroupLayout.makeHStretch());
        ((GroupLayout)getLayoutManager()).setGap(40);

        MessageBundle msgs =
            ctx.getMessageManager().getBundle(BankCodes.BANK_MSGS);

        BContainer lcont = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.LEFT));
        lcont.setPreferredSize(new Dimension(310, 50));
        lcont.add(new BLabel(msgs.get("m.great_offers"), "bank_title"));
        add(lcont, GroupLayout.FIXED);

//         TableLayout tlay = new TableLayout(3, 5, 15);
//         tlay.setHorizontalAlignment(TableLayout.CENTER);
//         BContainer ocont = new BContainer(tlay);
//         for (int ii = 0; ii < PACKAGES.length; ii++) {
//             BLabel coins = new BLabel(String.valueOf(PACKAGES[ii]));
//             coins.setIcon(BangUI.coinIcon);
//             ocont.add(coins);
//             ocont.add(new BLabel(msgs.get("m.for")));
//             ocont.add(new BLabel(msgs.get("m.cost_" + PACKAGES[ii]),
//                                  "offer_right"));
//         }
//         add(ocont);

        add(new BLabel(msgs.get("m.not_yet_available"), "offer_unavail"));

        BContainer bcont = new BContainer(
            GroupLayout.makeVert(GroupLayout.CENTER));
        BButton buy = new BButton(msgs.get("m.purchase"));
        buy.setStyleClass("huge_button");
        buy.setEnabled(false);
        bcont.add(buy);
        add(bcont, GroupLayout.FIXED);

        // TODO: wire up Buy to the web browser
    }

    protected static final int[] PACKAGES = { 12, 42, 400 };
}
