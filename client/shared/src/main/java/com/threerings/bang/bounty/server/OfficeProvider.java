//
// $Id$

package com.threerings.bang.bounty.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.bounty.client.OfficeService;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.data.BangConfig;

/**
 * Defines the server-side of the {@link OfficeService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from OfficeService.java.")
public interface OfficeProvider extends InvocationProvider
{
    /**
     * Handles a {@link OfficeService#testBountyGame} request.
     */
    void testBountyGame (PlayerObject caller, BangConfig arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;
}
