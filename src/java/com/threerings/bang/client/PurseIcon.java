//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.icon.ImageIcon;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Purse;
import com.threerings.bang.util.BangContext;

/**
 * Displays a purse in the inventory display.
 */
public class PurseIcon extends ItemIcon
{
    @Override // documentation inherited
    protected void configureLabel (BangContext ctx)
    {
        Purse purse = (Purse)_item;
        setIcon(new ImageIcon(ctx.loadImage("ui/unknown_item.png")));
        String mkey = "m." + Purse.PURSE_TYPES[purse.getTownIndex()];
        setText(ctx.xlate(BangCodes.GOODS_MSGS, mkey));
    }
}
