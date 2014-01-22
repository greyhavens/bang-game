//
// $Id$

package com.threerings.bang.saloon.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.client.ParlorService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorInfo;

/**
 * Defines the server-side of the {@link ParlorService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from ParlorService.java.")
public interface ParlorProvider extends InvocationProvider
{
    /**
     * Handles a {@link ParlorService#bootPlayer} request.
     */
    void bootPlayer (PlayerObject caller, int arg1);

    /**
     * Handles a {@link ParlorService#findSaloonMatch} request.
     */
    void findSaloonMatch (PlayerObject caller, Criterion arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ParlorService#leaveSaloonMatch} request.
     */
    void leaveSaloonMatch (PlayerObject caller, int arg1);

    /**
     * Handles a {@link ParlorService#updateParlorConfig} request.
     */
    void updateParlorConfig (PlayerObject caller, ParlorInfo arg1, boolean arg2);

    /**
     * Handles a {@link ParlorService#updateParlorPassword} request.
     */
    void updateParlorPassword (PlayerObject caller, String arg1);
}
