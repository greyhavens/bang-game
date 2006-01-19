//
// $Id$

package com.threerings.bang.client;

import com.jme.image.Image;
import com.jme.renderer.Renderer;
import com.jmex.bui.BWindow;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.RenderUtil;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.util.BangContext;

/**
 * Handles some shared stuff for our shop views (General Store, Bank, Barber,
 * etc.)
 */
public abstract class ShopView extends BWindow
    implements PlaceView
{
    public ShopView (BangContext ctx, String ident)
    {
        super(ctx.getStyleSheet(), new AbsoluteLayout());
        setStyleClass("shop_view");
        _ctx = ctx;

        // load up our background, shopkeep and other images
        String tpath = "ui/" + ident + "/" + _ctx.getUserObject().townId;
        _background = _ctx.loadImage("ui/" + ident + "/background.png");
        _shopkeep = _ctx.loadImage(tpath + "/shopkeep.png");
        _shopkbg = _ctx.loadImage(tpath + "/shopkeep_bg.png");
        _shop = _ctx.loadImage(tpath + "/shop.png");
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
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

    protected BangContext _ctx;

    protected Image _background, _shopkeep, _shopkbg, _shop;
}
