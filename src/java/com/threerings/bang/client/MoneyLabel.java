//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.ImageIcon;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.util.BangContext;

/**
 * Displays a quantity of scrip and gold.
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
     * Configures the quantity of scrip and gold displayed by this label.
     */
    public void setMoney (int scrip, int gold, boolean animate)
    {
        // TODO: animate and bling!
        _scrip.setText(String.valueOf(scrip));
        _gold.setText(String.valueOf(gold));
    }

    protected void createLabels (BangContext ctx)
    {
        add(_scrip = new BLabel(""));
        _scrip.setIcon(new ImageIcon(ctx.loadImage("ui/scrip.png")));
        add(_gold = new BLabel(""));
        _gold.setIcon(new ImageIcon(ctx.loadImage("ui/gold.png")));
    }

    protected BangContext _ctx;
    protected BLabel _label, _scrip, _gold;
}
