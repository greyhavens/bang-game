//
// $Id$

package com.threerings.bang.saloon.server;

import com.threerings.bang.data.Handle;
import com.threerings.bang.saloon.client.SaloonService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link SaloonService}.
 */
public interface SaloonProvider extends InvocationProvider
{
    /**
     * Handles a {@link SaloonService#createParlor} request.
     */
    public void createParlor (ClientObject caller, ParlorInfo.Type arg1, String arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link SaloonService#findMatch} request.
     */
    public void findMatch (ClientObject caller, Criterion arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link SaloonService#joinParlor} request.
     */
    public void joinParlor (ClientObject caller, Handle arg1, String arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link SaloonService#leaveMatch} request.
     */
    public void leaveMatch (ClientObject caller, int arg1);
}
