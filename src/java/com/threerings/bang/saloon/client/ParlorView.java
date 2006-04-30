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

//     /**
//      * Called by the controller to instruct us to display the pending match
//      * view when we have requested to play a game.
//      */
//     public void displayMatchView (int matchOid)
//     {
//         // remove our criterion view
//         if (_crview.isAdded()) {
//             remove(_crview);
//             remove(_soon);
//         }

//         // this should never happen, but just to be ultra-robust
//         if (_mview != null) {
//             remove(_mview);
//             _mview = null;
//         }

//         // display a match view for this pending match
//         add(_mview = new MatchView(_ctx, _ctrl, matchOid), MATCH_RECT);
//     }

//     /**
//      * Called by the match view if the player cancels their pending match.
//      * Redisplays the criterion view.
//      */
//     public void clearMatchView (String status)
//     {
//         // out with the old match view
//         if (_mview != null) {
//             remove(_mview);
//             _mview = null;
//         }

//         // redisplay the criterion view
//         if (!_crview.isAdded()) {
//             add(_crview, CRIT_RECT);
//             add(_soon, SOON_LOC);
//         }

//         setStatus(status);
//     }

//     public void findMatchFailed (String reason)
//     {
//         _crview.reenable();
//         _status.setStatus(_msgs.xlate(reason), true);
//     }

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
        _gconfig.willEnterPlace((ParlorObject)plobj);
        _config.willEnterPlace((ParlorObject)plobj);
        _chat.willEnterPlace(plobj);
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        _gconfig.didLeavePlace();
        _config.didLeavePlace();
        _chat.didLeavePlace(plobj);
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

    protected ParlorController _ctrl;
    protected ParlorGameConfigView _gconfig;
    protected ParlorConfigView _config;
    protected PlaceChatView _chat;
    protected StatusLabel _status;

    protected BImage _bgoverlay;

    protected static final Rectangle GAME_RECT =
        new Rectangle(102, 315, 410, 232);
}
