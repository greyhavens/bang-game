//
// $Id$

package com.threerings.bang.store.server;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link StoreService}.
 */
public interface StoreProvider extends InvocationProvider
{
    /**
     * Handles a {@link StoreService#buyGood} request.
     */
    public void buyGood (ClientObject caller, String arg1, Object[] arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;
}
