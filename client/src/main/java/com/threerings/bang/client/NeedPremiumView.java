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

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Star;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;
import com.threerings.bang.util.PaymentType;

import com.threerings.bang.store.data.StarGood;

/**
 * Notifies the player that they need the onetime purchase to access some content.
 */
public class NeedPremiumView extends BDecoratedWindow
    implements ActionListener
{
    /**
     * Potentially shows the need onetime view if the supplied purchase error message indicates
     * that the user needs the onetime pass to complete their purchase.
     *
     * @return true if the dialog was shown, false if not.
     */
    public static boolean maybeShowNeedPremium (BangContext ctx, String error)
    {
        if (!BangCodes.E_INSUFFICIENT_COINS.equals(error) &&
            !BangCodes.E_LACK_ONETIME.equals(error)) {
            return false;
        }
        ctx.getBangClient().displayPopup(new NeedPremiumView(ctx), true, WIDTH_HINT);
        return true;
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

    protected NeedPremiumView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        setModal(true);
        ((GroupLayout)getLayoutManager()).setGap(20);

        _ctx = ctx;
        MessageBundle msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        String type = DeploymentConfig.getPaymentType().toString().toLowerCase();
        setTitle(msgs.get("m.n" + type + "_title"));

        // onetime purchase gets a little box of fancy stuff
        if (DeploymentConfig.getPaymentType() == PaymentType.ONETIME) {
            add(createOnetimePalette());
        }

        add(new BLabel(msgs.get("m.n" + type + "_info")));

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)bcont.getLayoutManager()).setGap(25);
        bcont.add(new BButton(msgs.get("m.n" + type + "_get"), this, "get_onetime"));
        bcont.add(new BButton(msgs.get("m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);
    }

    protected IconPalette createOnetimePalette ()
    {
        IconPalette bits = new IconPalette(null, 3, 1, PaletteIcon.ICON_SIZE, 0);
        bits.setShowNavigation(false);
        bits.setPaintBorder(true);
        bits.setPaintBackground(true);

        // a steam gunman
        PaletteIcon icon = new PaletteIcon();
        icon.setText("New Units!");
        icon.setIcon(BangUI.getUnitIcon(UnitConfig.getConfig("frontier_town/steamgunman", true)));
        bits.addIcon(icon);

        // a deputy badge
        icon = new PaletteIcon();
        icon.setText("All the Bounties!");
        icon.setIcon(new StarGood(1, Star.Difficulty.EXTREME).createIcon(_ctx, null));
        bits.addIcon(icon);

        // a trickster raven
        icon = new PaletteIcon();
        icon.setText("More Big Shots!");
        icon.setIcon(BangUI.getUnitIcon(UnitConfig.getConfig("indian_post/tricksterraven", true)));
        bits.addIcon(icon);

        return bits;
    }

    protected BangContext _ctx;

    protected static final int WIDTH_HINT = 550;
}
