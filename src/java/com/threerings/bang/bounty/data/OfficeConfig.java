//
// $Id$

package com.threerings.bang.bounty.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.bounty.client.OfficeController;

/**
 * Defines the configuration for the Sheriff's Office.
 */
public class OfficeConfig extends PlaceConfig
{
    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new OfficeController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.bounty.server.OfficeManager";
    }
}
