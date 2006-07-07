//
// $Id$

package com.threerings.bang.station.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.util.Name;

/**
 * Provides Traion Station-related functionality.
 */
public interface StationService extends InvocationService
{
    /**
     * Requests the ticket to this client's next available station be
     * purchased.
     */
    public void buyTicket (Client client, ConfirmListener listener);
}
