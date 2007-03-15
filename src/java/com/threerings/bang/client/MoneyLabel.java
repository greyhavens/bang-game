//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * Displays a quantity of scrip and coins.
 */
public class MoneyLabel extends BContainer
{
    public MoneyLabel (BangContext ctx)
    {
        this(ctx, false);
    }

    public MoneyLabel (BangContext ctx, boolean compact)
    {
        int gap = compact ? 2 : 5;
        _ctx = ctx;
        setLayoutManager(new BorderLayout(gap, gap));
        add(_scrip = new BLabel(BangUI.scripIcon), BorderLayout.WEST);
        _scrip.setIconTextGap(gap);
        _scrip.setStyleClass("money_label");
        _scrip.setTooltipText(ctx.xlate(BangCodes.BANG_MSGS, "m.scrip_tip"));
        add(_coins = new BLabel(BangUI.coinIcon), BorderLayout.CENTER);
        _coins.setIconTextGap(gap);
        _coins.setStyleClass("money_label");
        _coins.setTooltipText(ctx.xlate(BangCodes.BANG_MSGS, "m.coin_tip"));

        setMoney(0, 0, false);
    }

    /**
     * Configures the quantity of scrip and coins displayed by this label.
     */
    public void setMoney (int scrip, int coins, boolean animate)
    {
        // TODO: animate and bling!
        _scrip.setText(String.valueOf(scrip));
        _coins.setText(String.valueOf(coins));
    }

    /**
     * Configures the style class of our scrip and coin labels.
     */
    public void setStyleClass (String styleClass)
    {
        _scrip.setStyleClass(styleClass);
        _coins.setStyleClass(styleClass);
    }

    protected BangContext _ctx;
    protected BLabel _scrip, _coins;
}
