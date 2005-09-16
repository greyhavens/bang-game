//
// $Id$

package com.threerings.bang.store.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.store.client.StoreService;
import com.threerings.bang.store.data.Good;

/**
 * Handles the server-side operation of the General Store.
 */
public class StoreManager
    implements StoreProvider
{
    // documentation inherited from interface StoreProvider
    public void buyGood (
        ClientObject caller, Good good, StoreService.ConfirmListener cl)
        throws InvocationException
    {
        throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
    }
}
