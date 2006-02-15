//
// $Id$

package com.threerings.bang.client;

import com.jme.image.Image;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Rectangle;
import com.jmex.bui.util.RenderUtil;

import com.threerings.util.MessageBundle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * Handles some shared stuff for our shop views (General Store, Bank, Barber,
 * etc.)
 */
public abstract class ShopView extends BWindow
    implements PlaceView, MainView
{
    /**
     * Displays the introduction help for this shop.
     */
    public void showHelp ()
    {
        if (_intro == null) {
            _intro = new BWindow(_ctx.getStyleSheet(), new BorderLayout(5, 15));
            _intro.setModal(true);
            _intro.setStyleClass("decoratedwindow");
            _intro.add(new BLabel(_msgs.get("m.intro_title"), "dialog_title"),
                       BorderLayout.NORTH);
            _intro.add(new BLabel(_msgs.get("m.intro_text"), "intro_body"),
                       BorderLayout.CENTER);
            BContainer btns = GroupLayout.makeHBox(GroupLayout.CENTER);
            btns.add(new BButton(_msgs.get("m.dismiss"), _ctrl, "dismiss"));
            _intro.add(btns, BorderLayout.SOUTH);
        }
        if (!_intro.isAdded()) {
            _ctx.getRootNode().addWindow(_intro);
            _intro.pack(600, -1);
            _intro.center();
        }
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    // documentation inherited from interface MainView
    public boolean allowsPopup (Type type)
    {
        return true;
    }

    /**
     * Creates a shop view and sets up its background, shopkeeper and other
     * standard imagery.
     *
     * @param ident the message bundle identifier and path under the
     * <code>ui</code> resource directory in which to find this shop's data.
     */
    protected ShopView (BangContext ctx, String ident)
    {
        super(ctx.getStyleSheet(), new AbsoluteLayout());
        setStyleClass("shop_view");
        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.black);
        _msgs = ctx.getMessageManager().getBundle(ident);

        // load up our background, shopkeep and other images
        String tpath = "ui/" + ident + "/" + _ctx.getUserObject().townId;
        _background = _ctx.loadImage("ui/" + ident + "/background.png");
        _shopkeep = _ctx.loadImage(tpath + "/shopkeep.png");
        _shopkbg = _ctx.loadImage(tpath + "/shopkeep_bg.png");
        _shop = _ctx.loadImage(tpath + "/shop.png");

        // add our town label
        String townId = _ctx.getUserObject().townId;
        add(new BLabel(_ctx.xlate(BangCodes.BANG_MSGS, "m." + townId),
                       "town_name_label"), new Rectangle(851, 637, 165, 20));
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // if this is the first time the player has entered this shop, show
        // them the intro popup
//         showHelp();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // clear out our intro if it's still showing
        if (_intro != null && _intro.isAdded()) {
            _intro.dismiss();
        }
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        super.renderBackground(renderer);

        RenderUtil.blendState.apply();
        RenderUtil.renderImage(_shopkbg, 12, _height-_shopkbg.getHeight()-12);
        RenderUtil.renderImage(_shopkeep, 12, _height-_shopkeep.getHeight()-12);
        RenderUtil.renderImage(_shop, _width-_shop.getWidth()-12,
                               _height-_shop.getHeight()-12);
        RenderUtil.renderImage(_background, 0, 0);
    }

    /**
     * Creates a button labeled "Help" that will show the introductory help
     * dialog.
     */
    protected BButton createHelpButton ()
    {
        return new BButton(_msgs.get("m.help"), _ctrl, "help");
    }

    protected ActionListener _ctrl = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            if ("dismiss".equals(event.getAction())) {
                _intro.dismiss();
            } else if ("help".equals(event.getAction())) {
                showHelp();
            }
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BWindow _intro;
    protected Image _background, _shopkeep, _shopkbg, _shop;
}
