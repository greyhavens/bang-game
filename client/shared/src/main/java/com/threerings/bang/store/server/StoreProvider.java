//
// $Id$

package com.threerings.bang.store.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.store.client.StoreService;

/**
 * Defines the server-side of the {@link StoreService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from StoreService.java.")
public interface StoreProvider extends InvocationProvider
{
    /**
     * Handles a {@link StoreService#buyGood} request.
     */
    void buyGood (PlayerObject caller, String arg1, Object[] arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;
}
