//
// $Id$

package com.threerings.bang.avatar.server;

import com.threerings.bang.avatar.client.BarberService;
import com.threerings.bang.avatar.data.Look;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link BarberService}.
 */
public interface BarberProvider extends InvocationProvider
{
    /**
     * Handles a {@link BarberService#purchaseLook} request.
     */
    public void purchaseLook (ClientObject caller, Look arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;
}
