//
// $Id$

package com.threerings.bang.store.client;

import java.util.Comparator;
import java.util.Arrays;
import java.util.Iterator;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.StoreObject;

/**
 * Displays a palette of purchasable goods.
 */
public class GoodsPalette extends IconPalette
{
    public GoodsPalette (BangContext ctx, GoodsInspector inspector)
    {
        super(inspector, 3, 1);
        _ctx = ctx;
    }

    public void init (StoreObject stobj)
    {
        _stobj = stobj;
        reinitGoods(true);
    }

    public void reinitGoods (boolean selectFirst)
    {
        clearSelections();
        removeAll();
        PlayerObject self = _ctx.getUserObject();

        Good[] goods = (Good[])
            _stobj.goods.toArray(new Good[_stobj.goods.size()]);
        Arrays.sort(goods, new Comparator<Good>() {
            public int compare (Good g1, Good g2) {
                return g1.getType().compareTo(g2.getType());
            }
        });
        for (int ii = 0; ii < goods.length; ii++) {
            if (goods[ii].isAvailable(self)) {
                add(new GoodsIcon(_ctx, goods[ii]));
            }
        }

        if (selectFirst && getComponentCount() > 0) {
            ((GoodsIcon)getComponent(0)).setSelected(true);
        }
    }

    public void shutdown ()
    {
    }

    protected BangContext _ctx;
    protected StoreObject _stobj;
}
