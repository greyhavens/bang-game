//
// $Id$

package com.threerings.bang.store.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.store.client.StoreService;

/**
 * Defines the server side of the {@link StoreService} interface.
 */
public interface StoreProvider extends InvocationProvider
{
    /**
     * Handles a {@link StoreService#buyGood} request.
     */
    public void buyGood (
        ClientObject caller, String type, StoreService.ConfirmListener cl)
        throws InvocationException;
}
