//
// $Id$

package com.threerings.bang.store.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.store.data.StoreObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the main interface for the General Store.
 */
public class StoreView extends ShopView
{
    public StoreView (BangContext ctx)
    {
        super(ctx, "store");

        // add our various interface components
        add(new BLabel(_msgs.get("m.intro_tip"), "shop_status"),
            new Rectangle(232, 640, 570, 35));

        add(new WalletLabel(_ctx, true), new Rectangle(40, 78, 150, 40));

        _inspector = new GoodsInspector(_ctx, this);
        add(_inspector, new Rectangle(268, 9, 500, 151));

        add(_goods = new GoodsPalette(_ctx, _inspector),
            new Rectangle(181, 140, 817, 495));

        add(new GoodsTabs(ctx, _goods), new Rectangle(48, 167, 133, 360));
        add(createHelpButton(), new Point(780, 25));
        add(new TownButton(ctx), new Point(870, 25));
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        // configure our goods palette and inspector
        _goods.init((StoreObject)plobj);
        _inspector.init((StoreObject)plobj);
    }

    @Override // documentation inherited
    protected Point getShopkeepNameLocation ()
    {
        return new Point(24, 516);
    }

    /**
     * Called by the palette when a good has been purchased.
     */
    protected void goodPurchased ()
    {
        _goods.reinitGoods(false);
    }

    protected GoodsPalette _goods;
    protected GoodsInspector _inspector;
}
