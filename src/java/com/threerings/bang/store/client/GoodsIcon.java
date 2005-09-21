//
// $Id$

package com.threerings.bang.store.client;

import com.jmex.bui.ImageIcon;

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

    protected BangContext _ctx;
    protected Good _good;
}
