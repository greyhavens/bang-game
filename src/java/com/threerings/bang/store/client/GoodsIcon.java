//
// $Id$

package com.threerings.bang.store.client;

import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Dimension;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.store.data.Good;

/**
 * Displays a salable good.
 */
public class GoodsIcon extends SelectableIcon
{
    public GoodsIcon (BangContext ctx, Good good)
    {
        _ctx = ctx;
        setOrientation(VERTICAL);
        setHorizontalAlignment(CENTER);
        setGood(good);
    }

    public Good getGood ()
    {
        return _good;
    }

    public void setGood (Good good)
    {
        _good = good;
        setText(_ctx.xlate(BangCodes.GOODS_MSGS, good.getName()));
        setIcon(new ImageIcon(_ctx.loadImage(good.getIconPath())));
    }

    public Dimension getPreferredSize (int whint, int hhint)
    {
        return ICON_SIZE;
    }

    protected BangContext _ctx;
    protected Good _good;

    protected static final Dimension ICON_SIZE = new Dimension(136, 156);
}
