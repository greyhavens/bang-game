//
// $Id$

package com.threerings.bang.store.client;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.StoreObject;

import java.util.Iterator;

/**
 * Displays a palette of purchasable goods.
 */
public class GoodsPalette extends IconPalette
{
    public GoodsPalette (BangContext ctx, GoodsInspector inspector)
    {
        super(inspector, 4, 1);
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

        // TODO: sort the goods by type
        for (Iterator iter = _stobj.goods.iterator(); iter.hasNext(); ) {
            Good good = (Good)iter.next();
            if (good.isAvailable(self)) {
                add(new GoodsIcon(_ctx, good));
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
