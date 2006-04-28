//
// $Id$

package com.threerings.bang.saloon.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.ParlorObject;

/**
 * Handles the client side of a Back Parlor.
 */
public class ParlorController extends PlaceController
{
    @Override // documentation inherited
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        super.init(ctx, config);
        _ctx = (BangContext)ctx;
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
        _parobj = (ParlorObject)plobj;
    }

    @Override // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_view = new ParlorView((BangContext)ctx, this));
    }

    protected BangContext _ctx;
    protected ParlorView _view;
    protected ParlorObject _parobj;
}
