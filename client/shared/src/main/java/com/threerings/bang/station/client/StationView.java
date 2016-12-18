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

/**
 * Displays the Train Station interface where players can purchase tickets to
 * the next town and travel between towns.
 */
public class StationView extends ShopView
{
    /** Used to display feedback. */
    public StatusLabel status;

    public StationView (BangContext ctx, StationController ctrl)
    {
        super(ctx, StationCodes.STATION_MSGS);

        // add our various interface components
        add(new BLabel(_msgs.get("m.intro_tip"), "shop_status"),
            new Rectangle(232, 656, 568, 35));

        add(new WalletLabel(ctx, true), new Rectangle(25, 40, 150, 40));
        add(createHelpButton(), new Point(780, 25));
        add(new TownButton(ctx), new Point(870, 25));
        add(status = new StatusLabel(ctx), new Rectangle(250, 10, 520, 50));
        status.setStyleClass("shop_status");
        status.setStatus(StationCodes.STATION_MSGS, "m.intro_status", false);

        // add our map display
        add(new MapView(_ctx, ctrl), new Point(59, 110));

        // and our ticket purchase display
        add(_tview = new TicketView(_ctx, status),
            new Rectangle(838, 107, 160, 526));
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

    @Override // documentation inherited
    protected Point getShopkeepNameLocation ()
    {
        return new Point(21, 528);
    }

    protected TicketView _tview;
}
