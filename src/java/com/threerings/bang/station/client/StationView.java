//
// $Id$

package com.threerings.bang.station.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.station.data.StationCodes;
import com.threerings.bang.station.data.StationObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the Train Station interface where players can purchase tickets to
 * the next town and travel between towns.
 */
public class StationView extends ShopView
{
    public StationView (BangContext ctx, StationController ctrl)
    {
        super(ctx, StationCodes.STATION_MSGS);

        // add our various interface components
        add(new BLabel(_msgs.get("m.intro_tip"), "shop_status"),
            new Rectangle(232, 661, 570, 35));

        String townId = _ctx.getUserObject().townId;
        add(new BLabel(_msgs.get("m.name_" + townId), "shopkeep_name_label"),
            new Rectangle(12, 513, 155, 25));

        add(new WalletLabel(_ctx, true), new Rectangle(40, 73, 150, 35));
        add(createHelpButton(), new Point(780, 25));
        add(new TownButton(ctx), new Point(870, 25));
        add(_status = new StatusLabel(ctx), new Rectangle(250, 10, 520, 50));
        _status.setStyleClass("shop_status");

        // add our map display
        add(new MapView(_ctx, ctrl), new Point(200, 180));

        // and our ticket purchase display
        add(_tview = new TicketView(_ctx, _status),
            new Rectangle(790, 120, 210, 490));
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
        _tview.init((StationObject)plobj);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
    }

    protected StatusLabel _status;
    protected TicketView _tview;
}
