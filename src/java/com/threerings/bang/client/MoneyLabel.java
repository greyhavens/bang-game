//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.bang.util.BangContext;

/**
 * Displays a quantity of scrip and coins.
 */
public class MoneyLabel extends BContainer
{
    public MoneyLabel (BangContext ctx)
    {
        _ctx = ctx;
        setLayoutManager(new BorderLayout(0, 0));
        add(_scrip = new BLabel(BangUI.scripIcon), BorderLayout.WEST);
        _scrip.setIconTextGap(5);
        _scrip.setStyleClass("money_label");
        add(_coins = new BLabel(BangUI.coinIcon), BorderLayout.CENTER);
        _coins.setIconTextGap(5);
        _coins.setStyleClass("money_label");
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

    protected BangContext _ctx;
    protected BLabel _scrip, _coins;
}
