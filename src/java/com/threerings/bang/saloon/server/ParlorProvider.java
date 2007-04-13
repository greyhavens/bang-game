//
// $Id$

package com.threerings.bang.saloon.server;

import com.threerings.bang.saloon.client.ParlorService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.presents.client.Client;
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
    public void bootPlayer (ClientObject caller, int arg1);

    /**
     * Handles a {@link ParlorService#findSaloonMatch} request.
     */
    public void findSaloonMatch (ClientObject caller, Criterion arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ParlorService#joinMatch} request.
     */
    public void joinMatch (ClientObject caller);

    /**
     * Handles a {@link ParlorService#leaveMatch} request.
     */
    public void leaveMatch (ClientObject caller);

    /**
     * Handles a {@link ParlorService#leaveSaloonMatch} request.
     */
    public void leaveSaloonMatch (ClientObject caller, int arg1);

    /**
     * Handles a {@link ParlorService#startMatchMaking} request.
     */
    public void startMatchMaking (ClientObject caller, ParlorGameConfig arg1, byte[] arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link ParlorService#updateGameConfig} request.
     */
    public void updateGameConfig (ClientObject caller, ParlorGameConfig arg1);

    /**
     * Handles a {@link ParlorService#updateParlorConfig} request.
     */
    public void updateParlorConfig (ClientObject caller, ParlorInfo arg1, boolean arg2);

    /**
     * Handles a {@link ParlorService#updateParlorPassword} request.
     */
    public void updateParlorPassword (ClientObject caller, String arg1);
}
