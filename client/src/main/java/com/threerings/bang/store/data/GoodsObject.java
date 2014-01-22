//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.DSet;

/**
 * An interface that provides access to a dobj's set of available goods.
 */
public interface GoodsObject
{
    /**
     * Returns the set of available goods.
     */
    public DSet<Good> getGoods();

    /**
     * Makes a request to the server to buy one of the goods.
     */
    public void buyGood (String type, Object[] args, InvocationService.ConfirmListener cl);
}
