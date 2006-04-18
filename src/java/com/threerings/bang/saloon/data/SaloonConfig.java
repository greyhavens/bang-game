//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.saloon.client.SaloonController;

/**
 * Defines the configuration for the Saloon.
 */
public class SaloonConfig extends PlaceConfig
{
    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new SaloonController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.saloon.server.SaloonManager";
    }
}
