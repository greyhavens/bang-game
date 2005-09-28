//
// $Id$

package com.threerings.bang.game.server;

import com.threerings.bang.game.client.BangService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link BangService}.
 */
public interface BangProvider extends InvocationProvider
{
    /**
     * Handles a {@link BangService#move} request.
     */
    public void move (ClientObject caller, int arg1, short arg2, short arg3, int arg4, InvocationService.InvocationListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link BangService#playCard} request.
     */
    public void playCard (ClientObject caller, int arg1, short arg2, short arg3);

    /**
     * Handles a {@link BangService#purchaseUnits} request.
     */
    public void purchaseUnits (ClientObject caller, String[] arg1);

    /**
     * Handles a {@link BangService#selectStarters} request.
     */
    public void selectStarters (ClientObject caller, int arg1, int[] arg2);
}
