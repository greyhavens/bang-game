//
// $Id$

package com.threerings.bang.avatar.data;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.avatar.client.BarberController;

/**
 * Defines the configuration for the Barber.
 */
public class BarberConfig extends PlaceConfig
{
    @Override // documentation inherited
    public Class getControllerClass ()
    {
        return BarberController.class;
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.avatar.server.BarberManager";
    }
}
