//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

/**
 * Notifies the player that they need the onetime purchase to access some content.
 */
public class NeedPremiumView extends BDecoratedWindow
    implements ActionListener
{
    public static final int WIDTH_HINT = 450;

    /**
     * Potentially shows the need onetime view if the supplied purchase error message indicates
     * that the user needs the onetime pass to complete their purchase.
     *
     * @return true if the dialog was shown, false if not.
     */
    public static boolean maybeShowNeedPremium (BangContext ctx, int coinCost, String error)
    {
        if (!BangCodes.E_INSUFFICIENT_FUNDS.equals(error) ||
            (DeploymentConfig.usesCoins() && coinCost <= ctx.getUserObject().coins) ||
            (DeploymentConfig.usesOneTime() && coinCost == 0)) {
            return false;
        }
        ctx.getBangClient().displayPopup(new NeedPremiumView(ctx), true, WIDTH_HINT);
        return true;
    }

    public NeedPremiumView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        setModal(true);
        ((GroupLayout)getLayoutManager()).setGap(20);

        _ctx = ctx;
        MessageBundle msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        String type = DeploymentConfig.getPaymentType().toString().toLowerCase();

        setTitle(msgs.get("m.n" + type + "_title"));
        add(new BLabel(msgs.get("m.n" + type + "_info")));

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)bcont.getLayoutManager()).setGap(25);
        bcont.add(new BButton(msgs.get("m.n" + type + "_get"), this, "get_onetime"));
        bcont.add(new BButton(msgs.get("m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // always clear
        _ctx.getBangClient().clearPopup(this, true);

        // only head to the bank if requested
        if ("get_onetime".equals(event.getAction())) {
            _ctx.showURL(DeploymentConfig.getBillingPassURL(_ctx, _ctx.getUserObject().townId));
        }
    }

    protected BangContext _ctx;
}
