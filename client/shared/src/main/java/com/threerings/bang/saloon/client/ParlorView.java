//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.StringUtil;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;

import com.threerings.bang.chat.client.PlaceChatView;
import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonCodes;

/**
 * Displays the main Parlor interface wherein a player can meet up with other
 * players to play games and inspect the various top-N lists.
 */
public class ParlorView extends ShopView
    implements ActionListener, AttributeChangeListener
{
    public ParlorView (BangContext ctx, ParlorController ctrl)
    {
        super(ctx, SaloonCodes.SALOON_MSGS);
        _ctrl = ctrl;

        // add our various interface components
        add(new BLabel(_msgs.get("m.title"), "shop_status"),
            new Rectangle(266, 656, 491, 33));
        add(new WalletLabel(ctx, true), new Rectangle(25, 40, 150, 40));
        add(createHelpButton(), new Point(800, 25));
        add(new BButton(_msgs.get("m.to_saloon"), this, "to_saloon"),
            new Point(870, 25));

        add(_config = new ParlorConfigIcons(_ctx), new Point(450, 263));
        add(_chat = new PlaceChatView(ctx, _msgs.get("m.parlor_chat")),
            new Rectangle(552, 78, 445, 551));

        add(_status = new StatusLabel(ctx), new Rectangle(276, 8, 500, 54));
        _status.setStyleClass("shop_status");
        _status.setText(_msgs.get("m.intro_tip"));

        ImageIcon banner = new ImageIcon(_ctx.loadImage("ui/saloon/parlor_settings.png"));
        add(new BLabel(banner), new Point(90, 260));

        // create our config view, but we'll add it later
        _tview = new TableGameView(_ctx, _status) {
            public boolean canCreate () {
                return _ctx.getUserObject().handle.equals(_parobj.info.creator) ||
                        !_parobj.onlyCreatorStart;
            }
        };
        //_gconfig = new ParlorGameConfigView(_ctx, _status);
        _crview = new CriterionView(_ctx, "saloon") {
            protected void findMatch (Criterion criterion) {
                _ctrl.findSaloonMatch(criterion);
            }
        };

        // allow the parlor owner to change the settings
        add(_settings = new BButton(_msgs.get("m.settings"), this, "settings"), new Point(340, 81));
        _settings.setVisible(false);
    }

    /**
     * Called by the controller to instruct us to display the pending saloon match view when we
     * have requested to play a game.
     */
    public void displaySaloonMatchView (int matchOid)
    {
        if (!_parobj.info.matched) {
            return;
        }
        // remove our criterion view
        if (_crview.isAdded()) {
            remove(_crview);
        }

        // this should never happen, but just to be ultra-robust
        if (_smview != null) {
            remove(_smview);
            _smview = null;
        }

        // display a match view for this pending match
        add(_smview = new MatchView(_ctx, matchOid) {
            protected void leaveMatch (int matchOid) {
                _ctrl.leaveSaloonMatch(matchOid);
            }
        }, MATCH_RECT);
    }

    /**
     * Called by the match view if the player cancels their pending saloon match.
     * Redisplays the criterion view.
     */
    public void clearSaloonMatchView (String status)
    {
        if (!_parobj.info.matched) {
            return;
        }
        // out with the old match view
        if (_smview != null) {
            remove(_smview);
            _smview = null;
        }

        // redisplay the criterion view
        if (!_crview.isAdded()) {
            add(_crview, SaloonView.CRIT_RECT);
        }

        setStatus(status);
    }

    public void findSaloonMatchFailed (String reason)
    {
        if (!_parobj.info.matched) {
            return;
        }
        _crview.reenable();
        _status.setStatus(_msgs.xlate(reason), true);
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
            BangBootstrapData bbd = (BangBootstrapData)_ctx.getClient().getBootstrapData();
            _ctx.getLocationDirector().moveTo(bbd.saloonOid);

        } else if ("settings".equals(event.getAction())) {
            _ctx.getBangClient().displayPopup(new ParlorConfigView(_ctx, _parobj), true);
        }
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (ParlorObject.ONLY_CREATOR_START.equals(event.getName())) {
            _tview.updateDisplay();
        }
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        _parobj = (ParlorObject)plobj;
        _parobj.addListener(this);

        _config.willEnterPlace(_parobj);
        if (!_parobj.info.matched) {
            // display some images over the ones baked into the background
            ImageIcon banner = new ImageIcon(
                _ctx.loadImage("ui/saloon/play_parlor_game.png"));
            add(new BLabel(banner), new Point(206, 578));
            _tview.willEnterPlace(_parobj.tableOid);
            add(_tview, MATCH_RECT);

        } else {
            clearSaloonMatchView(null);
        }

        if (_ctx.getUserObject().handle.equals(_parobj.info.creator)) {
            _settings.setVisible(true);
        }

        add(new BLabel(_parobj.info.server ? _msgs.get("m.server_parlor") :
                        _msgs.get(_parobj.info.matched ? "m.matched_name" : "m.parlor_name",
                        _parobj.info.creator), "parlor_label"),
                new Point(165, 263));
        add(new FolkView(_ctx, plobj, false, _parobj.info.gangId > 0),
            new Rectangle(95, 119, 407, 130));
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        _config.didLeavePlace();

        // unregister our chat display
        _chat.shutdown();

        if (_parobj != null) {
            if (!_parobj.info.matched) {
                _tview.didLeavePlace();
            }
            _parobj.removeListener(this);
            _parobj = null;
        }
    }

    @Override // documentation inherited
    protected Point getShopkeepNameLocation ()
    {
        return new Point(22, 554);
    }

    protected ParlorObject _parobj;
    protected ParlorController _ctrl;
    protected StatusLabel _status;
    protected PlaceChatView _chat;
    protected BButton _settings;

    protected TableGameView _tview;
    protected ParlorConfigIcons _config;
    protected CriterionView _crview;
    protected MatchView _smview;

    protected static final Rectangle MATCH_RECT =
        new Rectangle(77, 305, 440, 265);
}
