//
// $Id$

package com.threerings.bang.bank.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.bank.client.BankController;

/**
 * Defines the configuration for the Bank.
 */
public class BankConfig extends PlaceConfig
{
    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new BankController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.bank.server.BankManager";
    }
}
