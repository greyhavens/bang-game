//
// $Id$

package com.threerings.bang.store.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.ShopManager;

import com.threerings.bang.store.client.StoreService;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.StoreMarshaller;
import com.threerings.bang.store.data.StoreObject;

import static com.threerings.bang.Log.log;

/**
 * Handles the server-side operation of the General Store.
 */
public class StoreManager extends ShopManager
    implements StoreProvider
{
    // documentation inherited from interface StoreProvider
    public void buyGood (ClientObject caller, String type, Object[] args,
                         StoreService.ConfirmListener cl)
        throws InvocationException
    {
        PlayerObject user = requireShopEnabled(caller);

        // make sure we sell the good in question
        Good good = _stobj.goods.get(type);
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
        Provider provider = _goods.getProvider(user, good, args);
        if (provider == null) {
            log.warning("Unable to find provider for good [who=" + user.who() +
                        ", good=" + good + "].");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }
        provider.setListener(cl);
        provider.start();
    }

    @Override // from ShopManager
    protected String getIdent ()
    {
        return "store";
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new StoreObject();
    }

    @Override // from PlaceManager
    protected void didInit ()
    {
        super.didInit();

        // create our goods catalog
        _goods = new GoodsCatalog(BangServer.alogic);
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _stobj = (StoreObject)_plobj;
        _stobj.setService((StoreMarshaller)BangServer.invmgr.registerDispatcher(
                              new StoreDispatcher(this), false));

        // populate the store object with our salable goods
        _stobj.setGoods(new DSet<Good>(_goods.getGoods(ServerConfig.townId)));
    }

    protected StoreObject _stobj;
    protected GoodsCatalog _goods;
}
