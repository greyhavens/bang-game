//
// $Id$

package com.threerings.bang.station.server;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link StationService}.
 */
public interface StationProvider extends InvocationProvider
{
    /**
     * Handles a {@link StationService#activateTicket} request.
     */
    public void activateTicket (ClientObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link StationService#buyTicket} request.
     */
    public void buyTicket (ClientObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;
}
