//
// $Id$

package com.threerings.bang.bounty.server;

import com.threerings.bang.bounty.client.OfficeService;
import com.threerings.bang.bounty.data.BountyGameConfig;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link OfficeService}.
 */
public interface OfficeProvider extends InvocationProvider
{
    /**
     * Handles a {@link OfficeService#testBountyGame} request.
     */
    public void testBountyGame (ClientObject caller, BountyGameConfig arg1);
}
