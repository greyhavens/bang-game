//
// $Id$

package com.threerings.bang.store.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.BangUserObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;

import com.threerings.bang.store.client.StoreService;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.StoreMarshaller;
import com.threerings.bang.store.data.StoreObject;

import static com.threerings.bang.Log.log;

/**
 * Handles the server-side operation of the General Store.
 */
public class StoreManager extends PlaceManager
    implements StoreProvider
{
    // documentation inherited from interface StoreProvider
    public void buyGood (
        ClientObject caller, String type, StoreService.ConfirmListener cl)
        throws InvocationException
    {
        BangUserObject user = (BangUserObject)caller;

        // make sure we sell the good in question
        Good good = (Good)_stobj.goods.get(type);
        if (good == null) {
            log.warning("Requested to buy unknown good [who=" + user.who() +
                        ", type=" + type + "].");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        // validate that the client can buy this good
        if (!good.isAvailable(user)) {
            log.warning("Requested to buy unavailable good [who=" + user.who() +
                        ", good=" + good + "].");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        // create the appropriate provider and pass the buck to it
        Provider provider = Catalog.getProvider(user, good);
        if (provider == null) {
            log.warning("Unable to find provider for good [who=" + user.who() +
                        ", good=" + good + "].");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }
        provider.setListener(cl);
        provider.start();
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

        // register our invocation service
        _stobj = (StoreObject)_plobj;
        _stobj.setService((StoreMarshaller)BangServer.invmgr.registerDispatcher(
                              new StoreDispatcher(this), false));

        // populate the store object with our salable goods
        _stobj.setGoods(new DSet(Catalog.getGoods(ServerConfig.getTownId())));
    }

    protected StoreObject _stobj;
}
