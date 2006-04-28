//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.saloon.client.ParlorController;

/**
 * Defines the configuration for a Back Parlor.
 */
public class ParlorConfig extends PlaceConfig
{
    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new ParlorController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.saloon.server.ParlorManager";
    }
}
