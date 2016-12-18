//
// $Id$

package com.threerings.bang.saloon.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.client.TableGameService;
import com.threerings.bang.saloon.data.ParlorGameConfig;

/**
 * Defines the server-side of the {@link TableGameService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from TableGameService.java.")
public interface TableGameProvider extends InvocationProvider
{
    /**
     * Handles a {@link TableGameService#joinMatch} request.
     */
    void joinMatch (PlayerObject caller);

    /**
     * Handles a {@link TableGameService#joinMatchSlot} request.
     */
    void joinMatchSlot (PlayerObject caller, int arg1);

    /**
     * Handles a {@link TableGameService#leaveMatch} request.
     */
    void leaveMatch (PlayerObject caller);

    /**
     * Handles a {@link TableGameService#startMatchMaking} request.
     */
    void startMatchMaking (PlayerObject caller, ParlorGameConfig arg1, byte[] arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link TableGameService#updateGameConfig} request.
     */
    void updateGameConfig (PlayerObject caller, ParlorGameConfig arg1);
}
