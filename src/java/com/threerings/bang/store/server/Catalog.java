//
// $Id$

package com.threerings.bang.store.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.samskivert.util.ListUtil;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Purse;

import com.threerings.bang.store.data.CardPackGood;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.PurseGood;

import static com.threerings.bang.Log.log;

/**
 * Enumerates the various goods that can be purchased from the General
 * Shop and associates them with providers that are used to actually
 * create and deliver the goods when purchased.
 */
public class Catalog
{
    /**
     * Returns the goods that are available in the town in question.
     */
    public static Good[] getGoods (String townId)
    {
        ArrayList<Good> goods = new ArrayList<Good>();
        for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
            goods.addAll(_goods[tt].keySet());
            if (BangCodes.TOWN_IDS[tt].equals(townId)) {
                break;
            }
        }
        return goods.toArray(new Good[goods.size()]);
    }

    /**
     * Requests that a {@link Provider} be created to provide the specified
     * good to the specified user. Returns null if no provider is registered
     * for the good in question.
     */
    public static Provider getProvider (PlayerObject user, Good good)
        throws InvocationException
    {
        for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
            ProviderFactory factory = _goods[tt].get(good);
            if (factory != null) {
                return factory.createProvider(user, good);
            }
            if (BangCodes.TOWN_IDS[tt].equals(user.townId)) {
                break;
            }
        }
        return null;
    }

    /**
     * Registers a Good -> ProviderFactory mapping for the specified town.
     */
    protected static void registerGood (
        String townId, Good good, ProviderFactory factory)
    {
        int tidx = ListUtil.indexOf(BangCodes.TOWN_IDS, townId);
        if (tidx == -1) {
            log.warning("Requested to register good for invalid town " +
                        "[town=" + townId + ", good=" + good + "].");
            return;
        }
        _goods[tidx].put(good, factory);
    }

    /** Used to create a {@link Provider} for a particular {@link Good}. */
    protected static abstract class ProviderFactory {
        public abstract Provider createProvider (PlayerObject user, Good good)
            throws InvocationException;
    }

    /** Used for {@link PurseGood}s. */
    protected static class PurseProviderFactory extends ProviderFactory {
        public Provider createProvider (PlayerObject user, Good good)
            throws InvocationException {
            return new ItemProvider(user, good) {
                protected Item createItem () throws InvocationException {
                    int townIndex = ((PurseGood)_good).getTownIndex();
                    return new Purse(_user.playerId, townIndex);
                }
            };
        }

        protected int _townIndex;
    }

    /** Used for {@link CardPackGood}s. */
    protected static class CardPackProviderFactory extends ProviderFactory {
        public Provider createProvider (PlayerObject user, Good good)
            throws InvocationException {
            return new CardPackProvider(user, good);
        }
    }

    /** We can't create generic arrays, so we promote to a real class. */
    protected static class GoodsMap extends HashMap<Good,ProviderFactory> {
    }

    /** Contains mappings from {@link Good} to {@link ProviderFactory} for
     * the goods available in each town. */
    protected static GoodsMap[] _goods = new GoodsMap[BangCodes.TOWN_IDS.length];

    static {
        // create our goods mappings
        for (int ii = 0; ii < _goods.length; ii++) {
            _goods[ii] = new GoodsMap();
        }

        // register our purses
        ProviderFactory pf = new PurseProviderFactory();
        registerGood(BangCodes.FRONTIER_TOWN, new PurseGood(1, 1000, 1), pf);
        registerGood(BangCodes.INDIAN_VILLAGE, new PurseGood(2, 2500, 2), pf);
        registerGood(BangCodes.BOOM_TOWN, new PurseGood(3, 5000, 4), pf);
        registerGood(BangCodes.GHOST_TOWN, new PurseGood(4, 7500, 5), pf);
        registerGood(BangCodes.CITY_OF_GOLD, new PurseGood(5, 15000, 8), pf);

        // register our packs of cards
        pf = new CardPackProviderFactory();
        registerGood(BangCodes.FRONTIER_TOWN, new CardPackGood(5, 500, 0), pf);
//         registerGood(BangCodes.FRONTIER_TOWN, new CardPackGood(5, 500, 1), pf);
        registerGood(BangCodes.FRONTIER_TOWN, new CardPackGood(13, 1500, 2), pf);
        registerGood(BangCodes.FRONTIER_TOWN, new CardPackGood(52, 5000, 6), pf);
    }
}
