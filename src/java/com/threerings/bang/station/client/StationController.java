//
// $Id$

package com.threerings.bang.station.client;

import java.io.IOException;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.client.BangClient;
import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.data.BangCodes;
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
            String townId = cmd.substring(TAKE_TRAIN.length());
            if (BangClient.isTownActive(townId)) {
                takeTrain(townId);
            } else {
                activateTown(townId);
            }
        }
    }

    // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_view = new StationView((BangContext)ctx, this));
    }

    protected void takeTrain (final String townId)
    {
        // add a temporary client observer to handle logon failure
        final BangContext bctx = (BangContext)_ctx;
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

    protected void activateTown (final String townId)
    {
        // warn the user that we're going to download data, then do so
        final BangContext bctx = (BangContext)_ctx;
        OptionDialog.ResponseReceiver rr = new OptionDialog.ResponseReceiver() {
            public void resultPosted (int button, Object result) {
                try {
                    if (!BangClient.activateTown(bctx, townId)) {
                        // hrm, already activated? well OK...
                        takeTrain(townId);
                    }
                } catch (IOException ioe) {
                    String msg = ioe.getMessage();
                    if (msg == null) {
                        msg = "Unknown error";
                    }
                    if (!msg.startsWith("m.")) {
                        msg = MessageBundle.taint(msg);
                    }
                    msg = MessageBundle.compose("m.activation_failed", msg);
                    _view.status.setStatus(
                        StationCodes.STATION_MSGS, msg, true);
                }
            }
        };
        String msg = MessageBundle.qualify(BangCodes.BANG_MSGS, "m." + townId);
        msg = MessageBundle.compose("m.need_town_bundles", msg);
        OptionDialog.showConfirmDialog(
            bctx, StationCodes.STATION_MSGS, msg, new String[] { "m.ok" }, rr);
    }

    protected StationView _view;
}
