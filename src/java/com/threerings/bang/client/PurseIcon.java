//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.icon.ImageIcon;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Purse;
import com.threerings.bang.util.BasicContext;

/**
 * Displays a purse in the inventory display.
 */
public class PurseIcon extends ItemIcon
{
    @Override // documentation inherited
    protected void configureLabel (BasicContext ctx)
    {
        Purse purse = (Purse)_item;
        String type = Purse.PURSE_TYPES[purse.getTownIndex()];
        setIcon(new ImageIcon(ctx.loadImage("goods/purses/" + type + ".png")));
        setText(ctx.xlate(BangCodes.GOODS_MSGS, "m." + type));
    }
}
