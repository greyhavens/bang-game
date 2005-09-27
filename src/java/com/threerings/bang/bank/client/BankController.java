//
// $Id$

package com.threerings.bang.bank.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.util.BangContext;

/**
 * Manages the client side of the Bang! Bank.
 */
public class BankController extends PlaceController
{
    // documentation inherited
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        super.init(ctx, config);
        _ctx = (BangContext)ctx;
    }

    // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
    }

    // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_view = new BankView((BangContext)ctx));
    }

    protected BangContext _ctx;
    protected BankView _view;
}
