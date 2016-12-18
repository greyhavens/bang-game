//
// $Id$

package com.threerings.bang.store.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.PlayerObject;

/**
 * Provides invocation services relating to the General Store.
 */
public interface StoreService extends InvocationService<PlayerObject>
{
    /**
     * Issues a request to purchase the specified good from the store.
     *
     * @param type the type of good to be purchased.
     * @param args custom arguments to be interpreted on a per-good basis.
     */
    public void buyGood (String type, Object[] args, ConfirmListener cl);
}
