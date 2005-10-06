//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.util.BangContext;

/**
 * Displays a quantity of scrip and coins.
 */
public class MoneyLabel extends BContainer
{
    public MoneyLabel (BangContext ctx)
    {
        _ctx = ctx;
        setLayoutManager(GroupLayout.makeHoriz(GroupLayout.LEFT));
        createLabels(ctx);
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

    protected void createLabels (BangContext ctx)
    {
        add(_scrip = new BLabel(BangUI.scripIcon));
        add(_coins = new BLabel(BangUI.coinIcon));
    }

    protected BangContext _ctx;
    protected BLabel _label, _scrip, _coins;
}
