//
// $Id$

package com.threerings.bang.saloon.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.client.SaloonService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorInfo;

/**
 * Defines the server-side of the {@link SaloonService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from SaloonService.java.")
public interface SaloonProvider extends InvocationProvider
{
    /**
     * Handles a {@link SaloonService#createParlor} request.
     */
    void createParlor (PlayerObject caller, ParlorInfo.Type arg1, String arg2, boolean arg3, InvocationService.ResultListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link SaloonService#findMatch} request.
     */
    void findMatch (PlayerObject caller, Criterion arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link SaloonService#joinParlor} request.
     */
    void joinParlor (PlayerObject caller, Handle arg1, String arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link SaloonService#leaveMatch} request.
     */
    void leaveMatch (PlayerObject caller, int arg1);
}
