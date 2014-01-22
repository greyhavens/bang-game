//
// $Id$

package com.threerings.bang.station.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.PlayerObject;

/**
 * Provides Train Station-related functionality.
 */
public interface StationService extends InvocationService<PlayerObject>
{
    /**
     * Requests the ticket to this client's next available station be
     * purchased.
     */
    public void buyTicket (ConfirmListener listener);

    /**
     * Requests that a free ticket be activated.
     */
    public void activateTicket (ConfirmListener listener);
}
