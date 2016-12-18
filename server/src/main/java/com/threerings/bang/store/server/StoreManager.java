//
// $Id$

package com.threerings.bang.store.server;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.util.Interval;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangInvoker;
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
@Singleton
public class StoreManager extends ShopManager
    implements StoreProvider
{
    // documentation inherited from interface StoreProvider
    public void buyGood (PlayerObject caller, String type, Object[] args,
                         StoreService.ConfirmListener cl)
        throws InvocationException
    {
        PlayerObject user = requireShopEnabled(caller);

        // make sure we sell the good in question
        Good good = _stobj.goods.get(type);
        if (good == null) {
            log.warning("Requested to buy unknown good", "who", user.who(), "type", type);
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        // validate that the client can buy this good
        if (!good.isAvailable(user) || !user.hasCharacter()) {
            log.warning("Requested to buy unavailable good", "who", user.who(), "good", good);
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        // create the appropriate provider and pass the buck to it
        Provider provider = _goods.getProvider(user, good, args);
        if (provider == null) {
            log.warning("Unable to find provider for good", "who", user.who(), "good", good);
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }
        provider.setListener(cl);
        _invoker.post(provider);
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
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _stobj = (StoreObject)_plobj;
        _stobj.setService(BangServer.invmgr.registerProvider(this, StoreMarshaller.class));

        ArrayList<Good> goods = _goods.getGoods(ServerConfig.townId);

        // filter out the pending items
        for (Iterator<Good> iter = goods.iterator(); iter.hasNext(); ) {
            Good good = iter.next();
            if (good.isPending(System.currentTimeMillis())) {
                _pending.add(good);
                iter.remove();
            }
        }

        // populate the store object with our salable goods
        _stobj.setGoods(new DSet<Good>(goods));

        // register our goods update interval
        new Interval(BangServer.omgr) {
            public void expired () {
                long now = System.currentTimeMillis();
                // remove expired goods
                for (Good good : _stobj.goods.toArrayList()) {
                    if (good.isExpired(now)) {
                        _stobj.removeFromGoods(good.getKey());
                    }
                }
                // add in former pending goods
                for (Iterator<Good> iter = _pending.iterator(); iter.hasNext(); ) {
                    Good good = iter.next();
                    if (good.isPending(now)) {
                        continue;
                    }
                    iter.remove();
                    if (!good.isExpired(now)) {
                        _stobj.addToGoods(good);
                    }
                }
            }
        }.schedule(UPDATE_GOODS_INTERVAL, true);
    }

    protected StoreObject _stobj;
    protected ArrayList<Good> _pending = new ArrayList<Good>();

    // dependencies
    @Inject protected BangInvoker _invoker;
    @Inject protected GoodsCatalog _goods;

    protected static final long UPDATE_GOODS_INTERVAL = 15 * 60 * 1000L;
}
