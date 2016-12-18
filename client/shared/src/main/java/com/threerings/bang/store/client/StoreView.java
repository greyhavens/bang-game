//
// $Id$

package com.threerings.bang.store.client;

import java.util.HashMap;

import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;
import com.jmex.bui.BLabel;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;

import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.store.data.CardTripletGood;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.StoreCodes;
import com.threerings.bang.store.data.StoreObject;

import com.threerings.crowd.data.PlaceObject;

/**
 * Displays the main interface for the General Store.
 */
public class StoreView extends ShopView
{
    public StoreView (BangContext ctx)
    {
        super(ctx, StoreCodes.STORE_MSGS);

        // add our various interface components
        add(new BLabel(_msgs.get("m.intro_tip"), "shop_status"),
            new Rectangle(232, 640, 570, 35));

        add(new WalletLabel(_ctx, true), new Rectangle(43, 82, 150, 40));

        add(_goods = new GoodsPalette(_ctx, 6, 3), new Rectangle(181, 140, 817, 495));

        add(_inspector = new GoodsInspector(_ctx, _goods), new Rectangle(272, 9, 500, 151));
        _goods.setInspector(_inspector);

        add(new GoodsTabs(ctx, _goods), new Rectangle(48, 167, 133, 360));
        add(createHelpButton(), new Point(805, 25));
        add(new TownButton(ctx), new Point(870, 25));
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        // configure our goods palette and inspector
        StoreObject stobj = (StoreObject)plobj;
        HashMap<String, Integer> cardquants = new HashMap<String, Integer>();
        PlayerObject pobj = _ctx.getUserObject();
        for (Item item : pobj.inventory) {
            if (item instanceof CardItem) {
                CardItem ci = (CardItem)item;
                cardquants.put(ci.getType(), ci.getQuantity());
            }
        }
        for (Good good : stobj.goods) {
            if (good instanceof CardTripletGood) {
                CardTripletGood ctg = (CardTripletGood)good;
                Integer quant = cardquants.get(ctg.getCardType());
                ctg.setQuantity((quant == null ? 0 : quant.intValue()));
            }
        }
        _goods.init(stobj);
        _inspector.init(stobj);
    }

    @Override // documentation inherited
    protected Point getShopkeepNameLocation ()
    {
        return new Point(24, 516);
    }

    protected GoodsPalette _goods;
    protected GoodsInspector _inspector;
}
