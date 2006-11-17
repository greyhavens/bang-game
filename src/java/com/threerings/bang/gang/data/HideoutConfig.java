//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.gang.client.HideoutController;

/**
 * Defines the configuration for the Hideout.
 */
public class HideoutConfig extends PlaceConfig
{
    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new HideoutController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.gang.server.HideoutManager";
    }
}
