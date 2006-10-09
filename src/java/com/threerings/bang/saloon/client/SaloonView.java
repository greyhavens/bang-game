//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BLabel;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.StringUtil;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;

/**
 * Displays the main Saloon interface wherein a player can meet up with other
 * players to play games and inspect the various top-N lists.
 */
public class SaloonView extends ShopView
{
    public SaloonView (BangContext ctx, SaloonController ctrl)
    {
        super(ctx, SaloonCodes.SALOON_MSGS);
        _ctrl = ctrl;

        // add our various interface components
        add(new BLabel(_msgs.get("m.title"), "shop_status"),
            new Rectangle(266, 656, 491, 33));
        add(new WalletLabel(ctx, true), new Rectangle(25, 40, 150, 40));
        add(createHelpButton(), new Point(800, 25));
        add(new TownButton(ctx), new Point(870, 25));
        add(_paper = new PaperView(ctx), new Rectangle(542, 68, 465, 576));

        add(_parlist = new ParlorList(ctx), PARLIST_RECT);

        add(_crview = new CriterionView(ctx, _ctrl), CRIT_RECT);
        add(_status = new StatusLabel(ctx), new Rectangle(276, 8, 500, 54));
        _status.setStyleClass("shop_status");
        _status.setText(getShopTip());

//         // add a test game button for developer testing
//         if (ctx.getUserObject().tokens.isAdmin()) {
//             BButton btn;
//             add(btn = new BButton(
//                     new ImageIcon(ctx.loadImage("ui/icons/dice.png")),
//                     _ctrl, SaloonController.TEST_GAME), new Point(895, 274));
//             btn.setStyleClass("arrow_button");
//         }
    }

    /**
     * Called by the controller to instruct us to display the pending match
     * view when we have requested to play a game.
     */
    public void displayMatchView (int matchOid)
    {
        // remove our criterion view
        if (_crview.isAdded()) {
            remove(_crview);
            remove(_parlist);
        }

        // this should never happen, but just to be ultra-robust
        if (_mview != null) {
            remove(_mview);
            _mview = null;
        }

        // display a match view for this pending match
        add(_mview = new MatchView(_ctx, _ctrl, matchOid), MATCH_RECT);
    }

    /**
     * Called by the match view if the player cancels their pending match.
     * Redisplays the criterion view.
     */
    public void clearMatchView (String status)
    {
        // out with the old match view
        if (_mview != null) {
            remove(_mview);
            _mview = null;
        }

        // redisplay the criterion view
        if (!_crview.isAdded()) {
            add(_crview, CRIT_RECT);
            add(_parlist, PARLIST_RECT);
        }

        setStatus(status);
    }

    public void findMatchFailed (String reason)
    {
        _crview.reenable();
        _status.setStatus(_msgs.xlate(reason), true);
    }

    public void setStatus (String status)
    {
        String msg = StringUtil.isBlank(status) ? "" : _msgs.xlate(status);
        _status.setStatus(msg, false);
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        _paper.init((SaloonObject)plobj);
        _parlist.willEnterPlace((SaloonObject)plobj);
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        _parlist.didLeavePlace();
    }

    @Override // documentation inherited
    protected Point getShopkeepNameLocation ()
    {
        return new Point(22, 554);
    }

    protected SaloonController _ctrl;
    protected PaperView _paper;
    protected StatusLabel _status;
    protected CriterionView _crview;
    protected MatchView _mview;
    protected ParlorList _parlist;

    protected static final Rectangle CRIT_RECT =
        new Rectangle(77, 305, 440, 255);
    protected static final Rectangle MATCH_RECT =
        new Rectangle(77, 114, 440, 456);
    protected static final Rectangle PARLIST_RECT =
        new Rectangle(95, 81, 407, 168);
}
