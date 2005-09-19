//
// $Id$

package com.threerings.bang.store.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.server.ServerConfig;

import com.threerings.bang.store.client.StoreService;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.StoreObject;

/**
 * Handles the server-side operation of the General Store.
 */
public class StoreManager extends PlaceManager
    implements StoreProvider
{
    // documentation inherited from interface StoreProvider
    public void buyGood (
        ClientObject caller, Good good, StoreService.ConfirmListener cl)
        throws InvocationException
    {
        throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
    }

    @Override // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return StoreObject.class;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // TODO: anything?
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        _stobj = (StoreObject)_plobj;
        _stobj.setGoods(new DSet(Catalog.getGoods(ServerConfig.getTownId())));
    }

    protected StoreObject _stobj;
}
