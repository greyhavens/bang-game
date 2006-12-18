//
// $Id$

package com.threerings.bang.bounty.server;

import com.threerings.bang.bounty.client.OfficeService;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link OfficeService}.
 */
public interface OfficeProvider extends InvocationProvider
{
    /**
     * Handles a {@link OfficeService#playBountyGame} request.
     */
    public void playBountyGame (ClientObject caller, String arg1, String arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link OfficeService#testBountyGame} request.
     */
    public void testBountyGame (ClientObject caller, BangConfig arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;
}
