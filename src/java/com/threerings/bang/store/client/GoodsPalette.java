//
// $Id$

package com.threerings.bang.store.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.samskivert.util.ListUtil;

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
        super(inspector, 6, 3, GoodsIcon.ICON_SIZE, 1);
        _ctx = ctx;
        // setAllowsEmptySelection(false);
    }

    public void init (StoreObject stobj)
    {
        _stobj = stobj;
        if (_filter != null) {
            reinitGoods(false);
        }
    }

    public void setFilter (Filter filter)
    {
        _filter = filter;
        if (_stobj != null) {
            reinitGoods(false);
        }
    }

    public void reinitGoods (boolean reselectPrevious)
    {
        Good sgood = (getSelectedIcon() == null) ? null : ((GoodsIcon)getSelectedIcon()).getGood();
        int opage = _page;
        clear();

        // filter out all matching goods
        ArrayList<Good> filtered = new ArrayList<Good>();
        PlayerObject self = _ctx.getUserObject();
        for (Good good : _stobj.goods) {
            if ((_filter == null || _filter.isValid(good)) &&
                good.isAvailable(self)) {
                filtered.add(good);
            }
        }

        // now sort and display them
        Good[] goods = filtered.toArray(new Good[filtered.size()]);
        Arrays.sort(goods);
        for (int ii = 0; ii < goods.length; ii++) {
            addIcon(new GoodsIcon(_ctx, goods[ii]));
        }

        // reselect the previously selected good if specified and it's still
        // there; otherwise, flip to the previous page (if it still exists)
        if (!isAdded() || _icons.isEmpty()) {
            return;
        }

        if (reselectPrevious) {
            if (sgood != null) {
                int sidx = ListUtil.indexOf(goods, sgood);
                if (sidx != -1) {
                    displayPage(sidx / (_rows * _cols), false, false);
                    _icons.get(sidx).setSelected(true);
                    return;

                } else if (opage * (_rows * _cols) < goods.length) {
                    displayPage(opage, false, false);
                }
            }
            // if we're trying to reselect our previous good but couldn't find it, don't select
            // anything and leave the old good shown in the inspector

        } else {
            // select the first thing on the current page
            _icons.get(_page * _rows * _cols).setSelected(true);
        }
    }

    public void shutdown ()
    {
    }

    protected BangContext _ctx;
    protected StoreObject _stobj;
    protected Filter _filter;
}
