//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.util.BangContext;

/**
 * Manages the client side of the Hideout.
 */
public class HideoutController extends PlaceController
{
    // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return new HideoutView((BangContext)ctx);
    }
}
