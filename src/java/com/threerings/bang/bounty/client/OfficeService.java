//
// $Id$

package com.threerings.bang.bounty.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.game.data.BangConfig;

/**
 * Defines the client side of the Sheriff's Office services.
 */
public interface OfficeService extends InvocationService
{
    /**
     * Requests to play the specified bounty game.
     */
    public void playBountyGame (
        Client client, String bounty, String game, InvocationListener listener);

    /**
     * Requests to test the sepecified bounty game configuration. Only available to admins.
     */
    public void testBountyGame (Client client, BangConfig config, InvocationListener listener);
}
