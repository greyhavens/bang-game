//
// $Id$

package com.threerings.bang.avatar.server;

import com.threerings.bang.avatar.client.BarberService;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.data.Handle;
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
     * Handles a {@link BarberService#changeHandle} request.
     */
    void changeHandle (ClientObject caller, Handle arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link BarberService#configureLook} request.
     */
    void configureLook (ClientObject caller, String arg1, int[] arg2);

    /**
     * Handles a {@link BarberService#purchaseLook} request.
     */
    void purchaseLook (ClientObject caller, LookConfig arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;
}
