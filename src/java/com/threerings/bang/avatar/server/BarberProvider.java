//
// $Id$

package com.threerings.bang.avatar.server;

import com.threerings.bang.avatar.client.BarberService;
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
     * Handles a {@link BarberService#configureLook} request.
     */
    public void configureLook (ClientObject caller, String arg1, int[] arg2);

    /**
     * Handles a {@link BarberService#purchaseLook} request.
     */
    public void purchaseLook (ClientObject caller, String arg1, int arg2, int arg3, String[] arg4, int[] arg5, InvocationService.ConfirmListener arg6)
        throws InvocationException;
}
