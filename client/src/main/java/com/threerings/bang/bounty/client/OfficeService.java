//
// $Id$

package com.threerings.bang.bounty.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.data.BangConfig;

/**
 * Defines the client side of the Sheriff's Office services.
 */
public interface OfficeService extends InvocationService<PlayerObject>
{
    /**
     * Requests to test the sepecified bounty game configuration. Only available to admins.
     */
    public void testBountyGame (BangConfig config, InvocationListener listener);
}
