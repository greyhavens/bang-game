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

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.bang.chat.client.PlaceChatView;
import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangBootstrapData;
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

        add(_config = new ParlorConfigView(_ctx),
            new Rectangle(103, 124, 410, 132));
        add(_chat = new PlaceChatView(_ctx, _msgs.get("m.parlor_chat")),
            new Rectangle(570, 75, 425, 535));

        add(_status = new StatusLabel(ctx), new Rectangle(276, 8, 500, 54));
        _status.setStyleClass("shop_status");
        _status.setText(_msgs.get("m.intro_tip"));

        // create our config view, but we'll add it later
        _gconfig = new ParlorGameConfigView(_ctx, _status);

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
            BangBootstrapData bbd = (BangBootstrapData)
                _ctx.getClient().getBootstrapData();
            _ctx.getLocationDirector().moveTo(bbd.saloonOid);
        }
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        _parobj = (ParlorObject)plobj;
        _parobj.addListener(_occlist);

        _gconfig.willEnterPlace(_parobj);
        _config.willEnterPlace(_parobj);
        _chat.willEnterPlace(plobj);

        // show the match view if there's a game already in progress
        if (_parobj.playerOids != null) {
            displayMatchView();
        } else {
            clearMatchView();
        }

        // if there are people in the parlor, report who they are
        StringBuffer occs = new StringBuffer();
        for (OccupantInfo info : _parobj.occupantInfo) {
            if (_ctx.getUserObject().handle.equals(info.username)) {
                continue;
            }
            if (occs.length() > 0) {
                occs.append(", ");
            }
            occs.append(info.username);
        }
        if (occs.length() > 0) {
            String msg = MessageBundle.tcompose("m.parlor_occs", occs);
            _chat.displayInfo(SaloonCodes.SALOON_MSGS, msg);
        }
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        _gconfig.didLeavePlace();
        _config.didLeavePlace();
        _chat.didLeavePlace(plobj);

        if (_parobj != null) {
            _parobj.removeListener(_occlist);
            _parobj = null;
        }
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

    @Override // documentation inherited
    protected Point getShopkeepNameLocation ()
    {
        return new Point(22, 554);
    }

    /** Listens for occupant additions and removals. */
    protected SetAdapter _occlist = new SetAdapter() {
        public void entryAdded (EntryAddedEvent event) {
            if (event.getName().equals(ParlorObject.OCCUPANT_INFO)) {
                OccupantInfo info = (OccupantInfo)event.getEntry();
                reportOccupant("m.occ_entered", info.username);
            }
        }
        public void entryRemoved (EntryRemovedEvent event) {
            if (event.getName().equals(ParlorObject.OCCUPANT_INFO)) {
                OccupantInfo info = (OccupantInfo)event.getOldEntry();
                reportOccupant("m.occ_left", info.username);
            }
        }
        protected void reportOccupant (String msg, Name who) {
            _chat.displayInfo(SaloonCodes.SALOON_MSGS,
                MessageBundle.tcompose(msg, who));
        }
    };

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
