//
// $Id$

package com.threerings.bang.avatar.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.avatar.data.Look;

/**
 * Provides Barber-related functionality.
 */
public interface BarberService extends InvocationService
{
    /**
     * Requests that the specified look be purchased.
     */
    public void purchaseLook (Client client, Look look, ConfirmListener cl);
}
