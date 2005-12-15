//
// $Id$

package com.threerings.bang.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.client.PlayerService;
import com.threerings.bang.data.BangCodes;

/**
 * Handles general player business, implements {@link PlayerProvider}.
 */
public class PlayerManager
    implements PlayerProvider
{
    /**
     * Initializes the player manager, and registers its invocation service.
     */
    public void init ()
    {
        // register ourselves as the provider of the (bootstrap) PlayerService
        BangServer.invmgr.registerDispatcher(new PlayerDispatcher(this), true);
    }

    // documentation inherited from interface PlayerProvider
    public void pickFirstBigShot (ClientObject caller, String type,
                                  PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        throw new InvocationException(BangCodes.INTERNAL_ERROR);
    }
}
