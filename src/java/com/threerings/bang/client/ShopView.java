//
// $Id$

package com.threerings.bang.client;

import java.util.ArrayList;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.RandomUtil;
import com.threerings.util.MessageBundle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.bui.BangHTMLView;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * Handles some shared stuff for our shop views (General Store, Bank, Barber, etc.)
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
            _intro = new BWindow(_ctx.getStyleSheet(), GroupLayout.makeVStretch()) {
                protected Dimension computePreferredSize (int wh, int hh) {
                    Dimension d = super.computePreferredSize(wh, hh);
                    d.width = Math.min(d.width, 500);
                    return d;
                }
            };
            ((GroupLayout)_intro.getLayoutManager()).setOffAxisPolicy(GroupLayout.CONSTRAIN);
            ((GroupLayout)_intro.getLayoutManager()).setGap(15);
            _intro.setModal(true);
            _intro.setStyleClass("decoratedwindow");
            _intro.add(new BLabel(_msgs.get("m.intro_title"), "window_title"), GroupLayout.FIXED);

            BangHTMLView html = new BangHTMLView();
            html.setStyleClass("intro_body");
            html.setContents(_msgs.get("m.intro_text"));
            _intro.add(html);

            BContainer btns = GroupLayout.makeHBox(GroupLayout.CENTER);
            btns.add(new BButton(_msgs.get("m.dismiss"), _ctrl, "dismiss"));
            _intro.add(btns, GroupLayout.FIXED);
        }
        if (!_intro.isAdded()) {
            _ctx.getBangClient().displayPopup(_intro, true, 500);
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
     * Creates a shop view and sets up its background, shopkeeper and other standard imagery.
     *
     * @param ident the message bundle identifier and path under the <code>ui</code> resource
     * directory in which to find this shop's data.
     */
    protected ShopView (BangContext ctx, String ident)
    {
        super(ctx.getStyleSheet(), new AbsoluteLayout());
        setStyleClass("shop_view");
        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.black);
        _msgs = ctx.getMessageManager().getBundle(ident);

        // this is town but not shop specific
        String townId = _ctx.getUserObject().townId;
        _shopkbg = _ctx.loadImage("ui/shop/" + townId + "/shopkeep_bg.png");

        // this is shop but not town specific
        _background = _ctx.loadImage("ui/" + ident + "/background.png");

        // these are town and shop specific
        String tpath = "ui/" + ident + "/" + townId;
        _shopkeep = _ctx.loadImage(tpath + "/shopkeep.png");
        _shopimg = _ctx.loadImage(tpath + "/shop.png");
        _shopname = _ctx.loadImage(tpath + "/sign.png");

        // if there's a custom name label for the shopkeep, use that
        _nameloc = getShopkeepNameLocation();
        if (_nameloc != null) {
            _keepname = _ctx.loadImage(tpath + "/shopkeep_name.png");
        }

        // add our town label
        add(new BLabel(_ctx.xlate(BangCodes.BANG_MSGS, "m." + townId), "town_name_label"),
            new Rectangle(851, 637, 165, 20));

        // add a blank button over the shop image that returns to the town
        _townBtn = new BButton(
            new BlankIcon(_shopimg.getWidth(), _shopimg.getHeight()), _ctrl, "to_town");
        _townBtn.setStyleClass("def_button");
        add(_townBtn, new Point(1012-_shopimg.getWidth(), 756-_shopimg.getHeight()));
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // reference our images
        _shopkbg.reference();
        _shopkeep.reference();
        _shopimg.reference();
        _shopname.reference();
        _background.reference();
        if (_keepname != null) {
            _keepname.reference();
        }

        // if this is the first time the player has entered this shop, show them the intro popup
//         showHelp();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // release our images
        _shopkbg.release();
        _shopkeep.release();
        _shopimg.release();
        _shopname.release();
        _background.release();
        if (_keepname != null) {
            _keepname.release();
        }

        // clear out our intro if it's still showing
        if (_intro != null && _intro.isAdded()) {
            _ctx.getBangClient().clearPopup(_intro, true);
        }
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        super.renderBackground(renderer);

        _shopkbg.render(renderer, 12, _height-_shopkbg.getHeight()-12, _alpha);
        _shopkeep.render(renderer, 12, _height-_shopkeep.getHeight()-12, _alpha);
        _shopimg.render(renderer, _width-_shopimg.getWidth()-12,
                        _height-_shopimg.getHeight()-12, _alpha);
        _background.render(renderer, 0, 0, _alpha);
        _shopname.render(renderer, 273, _height-_shopname.getHeight()-7, _alpha);

        if (_keepname != null) {
            _keepname.render(renderer, _nameloc.x, _nameloc.y, _alpha);
        }
    }

    /**
     * Creates a button labeled "Help" that will show the introductory help dialog.
     */
    protected BButton createHelpButton ()
    {
        return new BButton(_msgs.get("m.help"), _ctrl, "help");
    }

    /**
     * Gets a random tip to display upon entering this shop. The tip will be selected from a set of
     * generic tips and a set of shop-specific tips.
     */
    protected String getShopTip ()
    {
        ArrayList<String> tips = new ArrayList<String>();
        // get our shop specific tips
        _msgs.getAll("m.shop_tip.", tips, false);
        // get our global tips
        _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS).getAll("m.shop_tip.", tips, false);
        // return a random tip
        return RandomUtil.pickRandom(tips);
    }

    /**
     * Returns the location at which to render the shopkeep name or null if we don't need a
     * shopkeep name.
     */
    protected Point getShopkeepNameLocation ()
    {
        return null;
    }

    protected ActionListener _ctrl = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            if ("dismiss".equals(event.getAction())) {
                _ctx.getBangClient().clearPopup(_intro, true);
            } else if ("help".equals(event.getAction())) {
                showHelp();
            } else if ("to_town".equals(event.getAction())) {
                _townBtn.setEnabled(false);
                _ctx.getBangClient().showTownView();
            }
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BWindow _intro;
    protected BButton _townBtn;
    protected BImage _background, _shopimg, _shopname;
    protected BImage _shopkeep, _shopkbg, _keepname;
    protected Point _nameloc;
}
