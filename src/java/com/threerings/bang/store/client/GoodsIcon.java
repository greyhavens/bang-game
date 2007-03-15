//
// $Id$

package com.threerings.bang.store.client;

import java.util.ArrayList;

import com.threerings.presents.dobj.DObject;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.CardTripletGood;
import com.threerings.bang.store.data.Good;

/**
 * Displays a salable good.
 */
public class GoodsIcon extends PaletteIcon
{
    /** Contains our randomly selected color ids for colorized goods. */
    public int[] colorIds = new int[3];

    public GoodsIcon (BangContext ctx, DObject entity, Good good)
    {
        _ctx = ctx;
        _entity = entity;
        setGood(good);
    }

    public Good getGood ()
    {
        return _good;
    }

    public void setGood (Good good)
    {
        _good = good;

        if (_good instanceof CardTripletGood) {
            setFitted(true);
        }

        setIcon(_good.createIcon(_ctx, _entity, colorIds));
        setText(_ctx.xlate(BangCodes.GOODS_MSGS, good.getName()));
        String msg = MessageBundle.compose(
            "m.goods_icon", good.getName(), good.getToolTip());
        setTooltipText(_ctx.xlate(BangCodes.GOODS_MSGS, msg));
    }

    protected BangContext _ctx;
    protected DObject _entity;
    protected Good _good;
}
