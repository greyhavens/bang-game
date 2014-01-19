//
// $Id$

package com.threerings.bang.store.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.samskivert.util.ListUtil;

import com.threerings.presents.dobj.DObject;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.GoodsObject;

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

    public GoodsPalette (BangContext ctx, int columns, int rows)
    {
        super(null, columns, rows, GoodsIcon.ICON_SIZE, 1);
        _ctx = ctx;
        // setAllowsEmptySelection(false);
    }

    public void init (GoodsObject goodsobj)
    {
        _goodsobj = goodsobj;
        if (_filter != null) {
            reinitGoods(false);
        }
    }

    public void setFilter (Filter filter)
    {
        _filter = filter;
        if (_goodsobj != null) {
            reinitGoods(false);
        }
    }

    public void reinitGoods (boolean reselectPrevious)
    {
        // reuse existing icons when they require colorizations (both to avoid the expense of
        // recoloring again and to prevent a disconcerting change of all colors on screen)
        HashMap<Good, GoodsIcon> oicons = new HashMap<Good, GoodsIcon>();
        for (SelectableIcon icon : _icons) {
            GoodsIcon gicon = (GoodsIcon)icon;
            if (gicon.getGood().getColorizationClasses(_ctx) != null) {
                oicons.put(gicon.getGood(), gicon);
            }
        }

        Good sgood = (getSelectedIcon() == null) ? null : ((GoodsIcon)getSelectedIcon()).getGood();
        int opage = _page;
        clear();

        // filter out all matching goods
        ArrayList<Good> filtered = new ArrayList<Good>();
        for (Good good : _goodsobj.getGoods()) {
            if ((_filter == null || _filter.isValid(good)) && isAvailable(good)) {
                filtered.add(good);
            }
        }

        // now sort and display them
        Good[] goods = filtered.toArray(new Good[filtered.size()]);
        Arrays.sort(goods, Good.BY_SCRIP_COST);
        for (int ii = 0; ii < goods.length; ii++) {
            GoodsIcon icon = oicons.get(goods[ii]);
            if (icon == null) {
                icon = new GoodsIcon(_ctx, getColorEntity(), goods[ii]);
            }
            addIcon(icon);
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

        } else if (autoSelectFirstItem()) {
            // select the first thing on the current page
            _icons.get(_page * _rows * _cols).setSelected(true);
        }
    }

    public boolean autoSelectFirstItem ()
    {
        return true;
    }

    /**
     * Determines whether the specified good is available for purchase.
     */
    protected boolean isAvailable (Good good)
    {
        return good.isAvailable(_ctx.getUserObject());
    }

    /**
     * Returns the entity to use in determining which colors are available.
     */
    protected DObject getColorEntity ()
    {
        return _ctx.getUserObject();
    }

    protected BangContext _ctx;
    protected GoodsObject _goodsobj;
    protected Filter _filter;
}
