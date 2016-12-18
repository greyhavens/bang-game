//
// $Id$

package com.threerings.bang.game.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.client.BangService;

/**
 * Defines the server-side of the {@link BangService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BangService.java.")
public interface BangProvider extends InvocationProvider
{
    /**
     * Handles a {@link BangService#cancelOrder} request.
     */
    void cancelOrder (PlayerObject caller, int arg1);

    /**
     * Handles a {@link BangService#getBoard} request.
     */
    void getBoard (PlayerObject caller, BangService.BoardListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link BangService#order} request.
     */
    void order (PlayerObject caller, int arg1, short arg2, short arg3, int arg4, InvocationService.ResultListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link BangService#playCard} request.
     */
    void playCard (PlayerObject caller, int arg1, Object arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link BangService#reportPerformance} request.
     */
    void reportPerformance (PlayerObject caller, String arg1, String arg2, int[] arg3);

    /**
     * Handles a {@link BangService#selectTeam} request.
     */
    void selectTeam (PlayerObject caller, int arg1, String[] arg2, int[] arg3);
}
