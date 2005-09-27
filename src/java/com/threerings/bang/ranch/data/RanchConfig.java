//
// $Id$

package com.threerings.bang.ranch.data;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.ranch.client.RanchController;

/**
 * Defines the configuration for the Ranch.
 */
public class RanchConfig extends PlaceConfig
{
    @Override // documentation inherited
    public Class getControllerClass ()
    {
        return RanchController.class;
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.ranch.server.RanchManager";
    }
}
