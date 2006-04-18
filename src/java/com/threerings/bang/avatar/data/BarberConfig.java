//
// $Id$

package com.threerings.bang.avatar.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.avatar.client.BarberController;

/**
 * Defines the configuration for the Barber.
 */
public class BarberConfig extends PlaceConfig
{
    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new BarberController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.avatar.server.BarberManager";
    }
}
