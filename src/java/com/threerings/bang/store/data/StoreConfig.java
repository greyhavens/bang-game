//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.store.client.StoreController;

/**
 * Defines the configuration for the Bang! General Store.
 */
public class StoreConfig extends PlaceConfig
{
    @Override // documentation inherited
    public Class getControllerClass ()
    {
        return StoreController.class;
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.store.server.StoreManager";
    }
}
