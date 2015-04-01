//
// $Id$

package com.threerings.bang.saloon.server;

import com.threerings.bang.saloon.client.ParlorService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link ParlorService}.
 */
public interface ParlorProvider extends InvocationProvider
{
    /**
     * Handles a {@link ParlorService#bootPlayer} request.
     */
    void bootPlayer (ClientObject caller, int arg1);

    /**
     * Handles a {@link ParlorService#findSaloonMatch} request.
     */
    void findSaloonMatch (ClientObject caller, Criterion arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ParlorService#leaveSaloonMatch} request.
     */
    void leaveSaloonMatch (ClientObject caller, int arg1);

    /**
     * Handles a {@link ParlorService#updateParlorConfig} request.
     */
    void updateParlorConfig (ClientObject caller, ParlorInfo arg1, boolean arg2);

    /**
     * Handles a {@link ParlorService#updateParlorPassword} request.
     */
    void updateParlorPassword (ClientObject caller, String arg1);
}