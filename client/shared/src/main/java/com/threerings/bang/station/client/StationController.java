//
// $Id$

package com.threerings.bang.station.client;

import java.io.IOException;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.samskivert.util.Interval;
import com.samskivert.util.Tuple;
import com.threerings.util.MessageBundle;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.client.BangClient;
import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.LogonView;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.station.data.StationCodes;
import com.threerings.bang.station.data.StationObject;

/**
 * Manages the client side of the Train Station.
 */
public class StationController extends PlaceController
    implements ActionListener
{
    /** The prefix for commands requesting that we take the train to a new town. */
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

    @Override // from PlaceController
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_view = new StationView((BangContext)ctx, this));
    }

    @Override // from PlaceController
    protected void didInit ()
    {
        super.didInit();
        _ctx = (BangContext)super._ctx;
    }

    protected void takeTrain (final String townId)
    {
        _view.status.setStatus(StationCodes.STATION_MSGS, "m.taking_train", false);

        // if we have a ticket, just go
        if (BangCodes.FRONTIER_TOWN.equals(townId) || _ctx.getUserObject().holdsTicket(townId)) {
            connectToTown(townId, _ctx.getUserObject().townId);
            return;
        }

        // otherwise we'll have to first activate our free ticket
        ((StationObject)_plobj).service.activateTicket(new StationService.ConfirmListener() {
            public void requestProcessed () {
                connectToTown(townId, _ctx.getUserObject().townId);
            }
            public void requestFailed (String reason) {
                _view.status.setStatus(StationCodes.STATION_MSGS, reason, true);
                }
        });
    }

    protected void connectToTown (final String townId, final String oldTownId)
    {
        // add a temporary client observer to handle logon failure
        ClientAdapter obs = new ClientAdapter() {
            public void clientDidLogon (Client client) {
                BangPrefs.setLastTownId(_ctx.getUserObject().username.toString(), townId);
                _ctx.getClient().removeClientObserver(this);
            }
            public void clientFailedToLogon (Client client, Exception cause) {
                _ctx.getClient().removeClientObserver(this);
                recoverFromFailure(townId, oldTownId, cause);
            }
        };
        _ctx.getClient().addClientObserver(obs);
        _ctx.getBangClient().switchToTown(townId);
    }

    protected void recoverFromFailure (String townId, final String oldTownId, Exception cause)
    {
        Tuple<String,Boolean> msg = LogonView.decodeLogonException(_ctx, cause);

        // if we failed to connect, try going back to our old town
        if (msg.right && !townId.equals(oldTownId)) {
            _view.status.setStatus(StationCodes.STATION_MSGS, "m.failed_to_connect", true);
            // give 'em two seconds to read the message, then go back
            new Interval(_ctx.getApp()) {
                public void expired () {
                    connectToTown(oldTownId, oldTownId);
                }
            }.schedule(2000L);

        } else {
            _view.status.setStatus(BangAuthCodes.AUTH_MSGS, msg.left, true);
        }
    }

    protected void activateTown (final String townId)
    {
        // warn the user that we're going to download data, then do so
        OptionDialog.ResponseReceiver rr = new OptionDialog.ResponseReceiver() {
            public void resultPosted (int button, Object result) {
                try {
                    if (!BangClient.activateTown(_ctx, townId)) {
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
                    _view.status.setStatus(StationCodes.STATION_MSGS, msg, true);
                }
            }
        };
        String msg = MessageBundle.qualify(BangCodes.BANG_MSGS, "m." + townId);
        msg = MessageBundle.compose("m.need_town_bundles", msg);
        OptionDialog.showConfirmDialog(
            _ctx, StationCodes.STATION_MSGS, msg, new String[] { "m.ok" }, rr);
    }

    protected BangContext _ctx;
    protected StationView _view;
}
