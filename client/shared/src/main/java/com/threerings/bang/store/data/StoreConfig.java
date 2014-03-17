//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.store.client.StoreController;

/**
 * Defines the configuration for the Bang! General Store.
 */
public class StoreConfig extends PlaceConfig
{
    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new StoreController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.store.server.StoreManager";
    }
}
