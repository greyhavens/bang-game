//
// $Id$

package com.threerings.bang.store.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.util.BangContext;

/**
 * Manages the client side of the General Store.
 */
public class StoreController extends PlaceController
{
    // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return new StoreView((BangContext)ctx);
    }
}
