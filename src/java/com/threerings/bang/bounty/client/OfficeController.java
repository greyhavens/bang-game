//
// $Id$

package com.threerings.bang.bounty.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.util.BangContext;

/**
 * Manages the client side of the Sheriff's Office.
 */
public class OfficeController extends PlaceController
{
    // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return new OfficeView((BangContext)ctx);
    }
}
