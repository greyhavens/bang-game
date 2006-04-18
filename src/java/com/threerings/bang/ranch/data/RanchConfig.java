//
// $Id$

package com.threerings.bang.ranch.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.ranch.client.RanchController;

/**
 * Defines the configuration for the Ranch.
 */
public class RanchConfig extends PlaceConfig
{
    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new RanchController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.ranch.server.RanchManager";
    }
}
