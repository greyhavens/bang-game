//
// $Id$

package com.threerings.bang.store.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    /** Used to filter the display of goods. */
    public interface Filter
    {
        public boolean isValid (Good good);
    }

    public GoodsPalette (BangContext ctx, GoodsInspector inspector)
    {
        super(inspector, 6, 3, GoodsIcon.ICON_SIZE, 1, false);
        _ctx = ctx;
    }

    public void init (StoreObject stobj)
    {
        _stobj = stobj;
        if (_filter != null) {
            reinitGoods(true);
        }
    }

    public void setFilter (Filter filter)
    {
        _filter = filter;
        if (_stobj != null) {
            reinitGoods(true);
        }
    }

    public void reinitGoods (boolean selectFirst)
    {
        clear();

        // filter out all matching goods
        ArrayList<Good> filtered = new ArrayList<Good>();
        PlayerObject self = _ctx.getUserObject();
        for (Iterator iter = _stobj.goods.iterator(); iter.hasNext(); ) {
            Good good = (Good)iter.next();
            if ((_filter == null || _filter.isValid(good)) &&
                good.isAvailable(self)) {
                filtered.add(good);
            }
        }

        // now sort and display them
        Good[] goods = filtered.toArray(new Good[filtered.size()]);
        Arrays.sort(goods, new Comparator<Good>() {
            public int compare (Good g1, Good g2) {
                return g1.getType().compareTo(g2.getType());
            }
        });
        for (int ii = 0; ii < goods.length; ii++) {
            addIcon(new GoodsIcon(_ctx, goods[ii]));
        }

        if (isAdded() && selectFirst && _icons.size() > 0) {
            _icons.get(0).setSelected(true);
        }
    }

    public void shutdown ()
    {
    }

    protected BangContext _ctx;
    protected StoreObject _stobj;
    protected Filter _filter;
}
