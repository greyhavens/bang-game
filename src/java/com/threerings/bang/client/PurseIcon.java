//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.icon.ImageIcon;

import com.threerings.util.MessageBundle;

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
        setIcon(new ImageIcon(ctx.loadImage(purse.getIconPath())));
        setText(ctx.xlate(BangCodes.GOODS_MSGS, purse.getName()));
        setTooltipText(ctx.xlate(BangCodes.GOODS_MSGS, purse.getTooltip()));
    }
}
