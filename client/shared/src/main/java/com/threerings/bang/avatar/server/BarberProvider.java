//
// $Id$

package com.threerings.bang.avatar.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.avatar.client.BarberService;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

/**
 * Defines the server-side of the {@link BarberService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BarberService.java.")
public interface BarberProvider extends InvocationProvider
{
    /**
     * Handles a {@link BarberService#changeHandle} request.
     */
    void changeHandle (PlayerObject caller, Handle arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link BarberService#configureLook} request.
     */
    void configureLook (PlayerObject caller, String arg1, int[] arg2);

    /**
     * Handles a {@link BarberService#purchaseLook} request.
     */
    void purchaseLook (PlayerObject caller, LookConfig arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;
}
