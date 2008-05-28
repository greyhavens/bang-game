//
// $Id$

package com.threerings.bang.station.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides Train Station-related functionality.
 */
public interface StationService extends InvocationService
{
    /**
     * Requests the ticket to this client's next available station be
     * purchased.
     */
    public void buyTicket (Client client, ConfirmListener listener);

    /**
     * Requests that a free ticket be activated.
     */
    public void activateTicket (Client client, ConfirmListener listener);
}
