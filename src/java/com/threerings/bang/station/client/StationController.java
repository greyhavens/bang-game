//
// $Id$

package com.threerings.bang.station.client;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.station.data.StationCodes;

/**
 * Manages the client side of the Train Station.
 */
public class StationController extends PlaceController
    implements ActionListener
{
    /** The prefix for commands requesting that we take the train to a new
     * town. */
    public static final String TAKE_TRAIN = "take_train:";

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.startsWith(TAKE_TRAIN)) {
            final String townId = cmd.substring(TAKE_TRAIN.length());
            final BangContext bctx = (BangContext)_ctx;

            // TODO: check that we have the necessary resources and if not,
            // configure getdown and force an update

            // add a temporary client observer to handle logon failure
            ClientAdapter obs = new ClientAdapter() {
                public void clientDidLogon (Client client) {
                    BangPrefs.setLastTownId(
                        bctx.getUserObject().username.toString(), townId);
                    _ctx.getClient().removeClientObserver(this);
                }
                public void clientFailedToLogon (Client client,
                                                 Exception cause) {
                    _ctx.getClient().removeClientObserver(this);
                    // TODO: do the standard logon view message massaging
                    _view.status.setStatus(
                        BangAuthCodes.AUTH_MSGS, cause.getMessage(), true);
                    // TODO: try to go back to our old town?
                }
            };
            _ctx.getClient().addClientObserver(obs);

            // logon to the new server
            _view.status.setStatus(
                StationCodes.STATION_MSGS, "m.taking_train", false);
            bctx.getBangClient().switchToTown(townId);
        }
    }

    // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_view = new StationView((BangContext)ctx, this));
    }

    protected StationView _view;
}
