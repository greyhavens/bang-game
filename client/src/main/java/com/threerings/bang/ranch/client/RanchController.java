//
// $Id$

package com.threerings.bang.ranch.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.util.BangContext;

/**
 * Manages the client side of the Ranch.
 */
public class RanchController extends PlaceController
{
    // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return new RanchView((BangContext)ctx);
    }
}
