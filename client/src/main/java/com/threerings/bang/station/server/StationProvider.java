//
// $Id$

package com.threerings.bang.station.server;

import com.threerings.bang.station.client.StationService;
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
    void activateTicket (ClientObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link StationService#buyTicket} request.
     */
    void buyTicket (ClientObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;
}
