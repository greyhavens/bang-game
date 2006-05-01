//
// $Id$

package com.threerings.bang.saloon.client;

import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.StringUtil;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.PlaceChatView;
import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonCodes;

/**
 * Displays the main Parlor interface wherein a player can meet up with other
 * players to play games and inspect the various top-N lists.
 */
public class ParlorView extends ShopView
    implements ActionListener
{
    public ParlorView (BangContext ctx, ParlorController ctrl)
    {
        super(ctx, SaloonCodes.SALOON_MSGS);
        _ctrl = ctrl;

        // add our various interface components
        add(new BLabel(_msgs.get("m.title"), "shop_status"),
            new Rectangle(266, 656, 491, 33));
        add(new WalletLabel(ctx, true), new Rectangle(25, 37, 150, 40));
        add(createHelpButton(), new Point(800, 25));
        add(new BButton(_msgs.get("m.to_saloon"), this, "to_saloon"),
            new Point(870, 25));

        add(_gconfig = new ParlorGameConfigView(_ctx), GAME_RECT);
        add(_config = new ParlorConfigView(_ctx),
            new Rectangle(103, 124, 410, 132));
        add(_chat = new PlaceChatView(_ctx), new Rectangle(570, 75, 425, 535));

        add(_status = new StatusLabel(ctx), new Rectangle(276, 8, 500, 54));
        _status.setStyleClass("shop_status");
        _status.setText(_msgs.get("m.intro_tip"));

        // load up our extra background
        _bgoverlay = ctx.loadImage("ui/saloon/parlor_bg.png");
    }

    /**
     * Called by the controller to instruct us to display the pending match
     * view when we have requested to play a game.
     */
    public void displayMatchView ()
    {
        // remove our configuration view
        if (_gconfig.isAdded()) {
            remove(_gconfig);
        }

        // this should never happen, but just to be ultra-robust
        if (_mview != null) {
            remove(_mview);
            _mview = null;
        }

        // display a match view for this pending match
        add(_mview = new ParlorMatchView(_ctx, _parobj), GAME_RECT);
    }

    /**
     * Called by the match view if the player cancels their pending match.
     * Redisplays the criterion view.
     */
    public void clearMatchView ()
    {
        // out with the old match view
        if (_mview != null) {
            remove(_mview);
            _mview = null;
        }

        // redisplay the criterion view
        if (!_gconfig.isAdded()) {
            add(_gconfig, GAME_RECT);
        }
    }

    public void setStatus (String status)
    {
        String msg = StringUtil.isBlank(status) ? "" : _msgs.xlate(status);
        _status.setStatus(msg, false);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("to_saloon".equals(event.getAction())) {
            _ctx.getLocationDirector().moveBack();
        }
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        _parobj = (ParlorObject)plobj;
        _gconfig.willEnterPlace(_parobj);
        _config.willEnterPlace(_parobj);
        _chat.willEnterPlace(plobj);
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        _gconfig.didLeavePlace();
        _config.didLeavePlace();
        _chat.didLeavePlace(plobj);
        _parobj = null;
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _bgoverlay.reference();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _bgoverlay.release();
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        super.renderBackground(renderer);
        _bgoverlay.render(renderer, 39, 65, _alpha);
    }

    protected ParlorObject _parobj;
    protected ParlorController _ctrl;
    protected BImage _bgoverlay;
    protected StatusLabel _status;
    protected PlaceChatView _chat;

    protected ParlorGameConfigView _gconfig;
    protected ParlorConfigView _config;
    protected ParlorMatchView _mview;

    protected static final Rectangle GAME_RECT =
        new Rectangle(102, 315, 410, 232);
}
