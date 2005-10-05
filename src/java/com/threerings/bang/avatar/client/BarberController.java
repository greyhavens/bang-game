//
// $Id$

package com.threerings.bang.avatar.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.util.BangContext;

/**
 * Manages the client side of the Barber.
 */
public class BarberController extends PlaceController
{
    // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return new BarberView((BangContext)ctx);
    }
}
