//
// $Id$

package com.threerings.bang.store.client;

import com.jme.image.Image;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BWindow;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.RenderUtil;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.store.data.StoreObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the main interface for the General Store.
 */
public class StoreView extends BWindow
    implements PlaceView
{
    public StoreView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), new AbsoluteLayout());

        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);
        _msgs = ctx.getMessageManager().getBundle("store");

        String townId = _ctx.getUserObject().townId;

        // load up our background, shopkeep and other images
        _background = _ctx.loadImage("ui/store/background.png");
        _shopkeep = _ctx.loadImage("ui/store/" + townId + "/shopkeep.png");
        _shopkbg = _ctx.loadImage("ui/store/" + townId + "/shopkeep_bg.png");
        _shop = _ctx.loadImage("ui/store/" + townId + "/shop.png");

        // add our various interface components
        BLabel introtip = new BLabel(_msgs.get("m.intro_tip"));
        introtip.setStyleClass("shop_intro");
        add(introtip, new Rectangle(232, 640, 570, 35));

        add(new WalletLabel(_ctx, true), new Rectangle(40, 77, 150, 45));

        _inspector = new GoodsInspector(_ctx, this);
        add(_inspector, new Rectangle(268, 9, 500, 151));

        add(_goods = new GoodsPalette(_ctx, _inspector),
            new Rectangle(181, 168, 817, 468));

        add(new GoodsTabs(ctx, _goods), new Rectangle(48, 167, 133, 360));
        add(new TownButton(ctx), new Point(870, 25));
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        // configure our goods palette and inspector
        _goods.init((StoreObject)plobj);
        _inspector.init((StoreObject)plobj);
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    /**
     * Called by the palette when a good has been purchased.
     */
    protected void goodPurchased ()
    {
        _goods.reinitGoods(false);
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        super.renderBackground(renderer);

        int width = renderer.getWidth(), height = renderer.getHeight();
        RenderUtil.blendState.apply();
        RenderUtil.renderImage(_shopkbg, 12, height-_shopkbg.getHeight()-12);
        RenderUtil.renderImage(_shopkeep, 12, height-_shopkeep.getHeight()-12);
        RenderUtil.renderImage(_shop, width-_shop.getWidth()-12,
                               height-_shop.getHeight()-12);
        RenderUtil.renderImage(_background, 0, 0);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected GoodsPalette _goods;
    protected GoodsInspector _inspector;

    protected Image _background, _shopkeep, _shopkbg, _shop;
}
