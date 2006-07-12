//
// $Id$

package com.threerings.bang.station.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.TrainTicket;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.station.data.StationCodes;

/**
 * Displays the map of the different towns and manages the buttons that will
 * take the player between them.
 */
public class MapView extends BContainer
{
    public MapView (BangContext ctx, StationController ctrl)
    {
        super(new AbsoluteLayout());
        setStyleClass("station_map");
        _ctx = ctx;
        MessageBundle msgs = ctx.getMessageManager().getBundle(
            StationCodes.STATION_MSGS);
        String townId = ctx.getUserObject().townId;

        // add buttons or labels for each town
        _towns = new BComponent[TOWN_SPOTS.length];
        for (int ii = 0; ii < _towns.length; ii++) {
            if (BangCodes.TOWN_IDS[ii].equals(townId)) {
                _towns[ii] = new BLabel(msgs.get("m.you_are_here"));
            } else {
                _towns[ii] = new BButton(msgs.get("b.take_train"), ctrl,
                                         StationController.TAKE_TRAIN +
                                         BangCodes.TOWN_IDS[ii]);
                // frontier town is always enabled, the other towns all start
                // disabled and we'll enable them if the player has a ticket
                _towns[ii].setEnabled(ii == 0);
            }
            add(_towns[ii], TOWN_SPOTS[ii]);
        }

        enableTownButtons();
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _ctx.getUserObject().addListener(_enabler);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctx.getUserObject().removeListener(_enabler);
    }

    protected void enableTownButtons ()
    {
        for (int ii = 1; ii < _towns.length; ii++) {
            if (!(_towns[ii] instanceof BButton)) {
                continue;
            }
            _towns[ii].setEnabled(
                _ctx.getUserObject().holdsTicket(BangCodes.TOWN_IDS[ii]));
        }
    }

    /** Listens for additions to the player's inventory and reenables our town
     * buttons if they buy a ticket. */
    protected SetAdapter _enabler = new SetAdapter() {
        public void entryAdded (EntryAddedEvent event) {
            if (event.getName().equals(PlayerObject.INVENTORY)) {
                enableTownButtons();
            }
        }
    };

    protected BangContext _ctx;
    protected BComponent[] _towns;

    protected static final Point[] TOWN_SPOTS = {
        new Point(45, 115),
        new Point(270, 420),
    };
}
