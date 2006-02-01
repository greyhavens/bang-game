//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.StringUtil;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.SaloonCodes;

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
        add(new BLabel(_msgs.get("m.intro_tip"), "shop_status"),
            new Rectangle(232, 640, 570, 35));
        add(createHelpButton(), new Point(780, 25));
        add(new TownButton(ctx), new Point(870, 25));

        add(_crview = new CriterionView(ctx, _ctrl), new Point(700, 300));

        add(_status = new StatusLabel(ctx), new Rectangle(450, 80, 400, 50));
    }

    /**
     * Called by the controller to instruct us to display the pending match
     * view when we have requested to play a game.
     */
    public void displayMatchView (int matchOid)
    {
        remove(_crview);
        add(_mview = new MatchView(_ctx, _ctrl, matchOid), new Point(700, 300));
    }

    /**
     * Called by the match view if the player cancels their pending match.
     * Redisplays the criterion view.
     */
    public void clearMatchView (String status)
    {
        remove(_mview);
        _mview = null;
        add(_crview, new Point(700, 300));
        setStatus(status);
    }

    public void setStatus (String status)
    {
        String msg = StringUtil.isBlank(status) ? "" : _msgs.xlate(status);
        _status.setStatus(msg, false);
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    protected SaloonController _ctrl;
    protected StatusLabel _status;
    protected CriterionView _crview;
    protected MatchView _mview;
}
