//
// $Id$

package com.threerings.bang.station.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.station.client.StationService;

/**
 * Defines the server-side of the {@link StationService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from StationService.java.")
public interface StationProvider extends InvocationProvider
{
    /**
     * Handles a {@link StationService#activateTicket} request.
     */
    void activateTicket (PlayerObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link StationService#buyTicket} request.
     */
    void buyTicket (PlayerObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;
}
