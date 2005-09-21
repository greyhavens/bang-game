//
// $Id$

package com.threerings.bang.store.client;

import com.threerings.bang.client.bui.IconPalette;
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
        super(inspector);
        _ctx = ctx;
    }

    public void init (StoreObject stobj)
    {
        // TODO: sort the goods by type
        for (Iterator iter = stobj.goods.iterator(); iter.hasNext(); ) {
            add(new GoodsIcon(_ctx, (Good)iter.next()));
        }
        if (getComponentCount() > 0) {
            ((GoodsIcon)getComponent(0)).setSelected(true);
        }
    }

    public void shutdown ()
    {
    }

    protected BangContext _ctx;
}
