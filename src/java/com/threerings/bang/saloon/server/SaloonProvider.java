//
// $Id$

package com.threerings.bang.saloon.server;

import com.threerings.bang.saloon.client.SaloonService;
import com.threerings.bang.saloon.data.Criterion;
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
     * Handles a {@link SaloonService#findMatch} request.
     */
    public void findMatch (ClientObject caller, Criterion arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link SaloonService#leaveMatch} request.
     */
    public void leaveMatch (ClientObject caller, int arg1);
}
