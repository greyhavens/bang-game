//
// $Id$

package com.threerings.bang.station.client;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.util.BangContext;

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
            System.err.println("Taking train to " + townId + "...");
            // TODO
        }
    }

    // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return new StationView((BangContext)ctx, this);
    }
}
