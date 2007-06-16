//
// $Id$

package com.threerings.bang.saloon.server;

import com.threerings.bang.saloon.client.TableGameService;
import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link TableGameService}.
 */
public interface TableGameProvider extends InvocationProvider
{
    /**
     * Handles a {@link TableGameService#joinMatch} request.
     */
    public void joinMatch (ClientObject caller);

    /**
     * Handles a {@link TableGameService#joinMatchSlot} request.
     */
    public void joinMatchSlot (ClientObject caller, int arg1);

    /**
     * Handles a {@link TableGameService#leaveMatch} request.
     */
    public void leaveMatch (ClientObject caller);

    /**
     * Handles a {@link TableGameService#startMatchMaking} request.
     */
    public void startMatchMaking (ClientObject caller, ParlorGameConfig arg1, byte[] arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link TableGameService#updateGameConfig} request.
     */
    public void updateGameConfig (ClientObject caller, ParlorGameConfig arg1);
}
