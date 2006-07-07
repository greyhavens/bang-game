//
// $Id$

package com.threerings.bang.client;

import java.io.StringReader;
import java.util.logging.Level;
import javax.swing.text.html.HTMLDocument;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.HTMLView;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.util.MessageBundle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

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
            _intro = new BWindow(_ctx.getStyleSheet(),
                                 GroupLayout.makeVStretch()) {
                protected Dimension computePreferredSize (int wh, int hh) {
                    Dimension d = super.computePreferredSize(wh, hh);
                    d.width = Math.min(d.width, 500);
                    return d;
                }
            };
            ((GroupLayout)_intro.getLayoutManager()).setOffAxisPolicy(
                GroupLayout.CONSTRAIN);
            ((GroupLayout)_intro.getLayoutManager()).setGap(15);
            _intro.setModal(true);
            _intro.setStyleClass("decoratedwindow");
            _intro.add(new BLabel(_msgs.get("m.intro_title"), "window_title"),
                       GroupLayout.FIXED);

            // set up our HTML
            HTMLView html = new HTMLView();
            html.setStyleClass("intro_body");
            HTMLDocument doc = new HTMLDocument(BangUI.css);
            String text = _msgs.get("m.intro_text");
            try {
                html.getEditorKit().read(new StringReader(text), doc, 0);
                html.setContents(doc);
            } catch (Throwable t) {
                log.log(Level.WARNING, "Failed to parse shop help " +
                        "[contents=" + text + "].", t);
            }
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
        _shopimg = _ctx.loadImage(tpath + "/shop.png");
        _shopname = _ctx.loadImage(tpath + "/sign.png");

        // if there's a custom name label for the shopkeep, use that
        _nameloc = getShopkeepNameLocation();
        if (_nameloc != null) {
            _keepname = _ctx.loadImage(tpath + "/shopkeep_name.png");
        }

        // add our town label
        String townId = _ctx.getUserObject().townId;
        add(new BLabel(_ctx.xlate(BangCodes.BANG_MSGS, "m." + townId),
                       "town_name_label"), new Rectangle(851, 637, 165, 20));
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

        // if this is the first time the player has entered this shop, show
        // them the intro popup
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
        _shopkeep.render(
            renderer, 12, _height-_shopkeep.getHeight()-12, _alpha);
        _shopimg.render(renderer, _width-_shopimg.getWidth()-12,
                     _height-_shopimg.getHeight()-12, _alpha);
        _background.render(renderer, 0, 0, _alpha);
        _shopname.render(renderer, (_width-_shopname.getWidth())/2,
                         _height-_shopname.getHeight()-7, _alpha);

        if (_keepname != null) {
            _keepname.render(renderer, _nameloc.x, _nameloc.y, _alpha);
        }
    }

    /**
     * Creates a button labeled "Help" that will show the introductory help
     * dialog.
     */
    protected BButton createHelpButton ()
    {
        return new BButton(_msgs.get("m.help"), _ctrl, "help");
    }

    /**
     * Returns the location at which to render the shopkeep name or null if we
     * don't need a shopkeep name.
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
            }
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BWindow _intro;
    protected BImage _background, _shopimg, _shopname;
    protected BImage _shopkeep, _shopkbg, _keepname;
    protected Point _nameloc;
}
