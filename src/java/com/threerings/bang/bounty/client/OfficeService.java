//
// $Id$

package com.threerings.bang.bounty.client;

import com.threerings.presents.client.Client;

import com.threerings.bang.bounty.data.BountyGameConfig;

/**
 * Defines the client side of the Sheriff's Office services.
 */
public interface OfficeService
{
    /**
     * A service used by developers when creating and testing bounty games.
     */
    public void testBountyGame (Client client, BountyGameConfig config);
}
