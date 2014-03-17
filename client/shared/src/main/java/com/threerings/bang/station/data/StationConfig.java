//
// $Id$

package com.threerings.bang.station.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.station.client.StationController;

/**
 * Defines the configuration for the Train Station.
 */
public class StationConfig extends PlaceConfig
{
    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new StationController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.station.server.StationManager";
    }
}
