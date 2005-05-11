//
// $Id$

package com.threerings.bang.lobby.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.lobby.data.LobbyConfig;
import com.threerings.bang.util.BangContext;

/**
 * Manages the client side of a Bang! lobby.
 */
public class LobbyController extends PlaceController
{
    // documentation inherited
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        super.init(ctx, config);

        // cast our references
        _ctx = (BangContext)ctx;
        _config = (LobbyConfig)config;
    }

    // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
    }

    // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_view = new LobbyView((BangContext)ctx));
    }

    protected BangContext _ctx;
    protected LobbyConfig _config;
    protected LobbyView _view;
}
