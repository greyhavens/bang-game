//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.HideoutCodes;

/**
 * Extends {@link MoneyLabel} to include aces.
 */
public class GangMoneyLabel extends MoneyLabel
{
    public GangMoneyLabel (BangContext ctx)
    {
        super(ctx, false);

        add(_aces = new BLabel(BangUI.acesIcon), BorderLayout.EAST);
        _aces.setIconTextGap(5);
        _aces.setStyleClass("money_label");
        _aces.setTooltipText(ctx.xlate(HideoutCodes.HIDEOUT_MSGS, "m.ace_tip"));

        setMoney(0, 0, 0, false);
    }

    /**
     * Configures the quantity of scrip, coins, and aces displayed by this label.
     */
    public void setMoney (int scrip, int coins, int aces, boolean animate)
    {
        super.setMoney(scrip, coins, animate);
        _aces.setText(String.valueOf(aces));
    }

    @Override // from MoneyLabel
    public void setStyleClass (String styleClass)
    {
        super.setStyleClass(styleClass);
        _aces.setStyleClass(styleClass);
    }

    protected BLabel _aces;
}
