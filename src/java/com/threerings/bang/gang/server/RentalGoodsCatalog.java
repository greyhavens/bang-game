//
// $Id$

package com.threerings.bang.gang.server;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.server.InvocationException;
import com.threerings.presents.data.InvocationCodes;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangUtil;

import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.RentalGood;
import com.threerings.bang.gang.data.TicketGood;

import com.threerings.bang.station.data.StationCodes;
import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.UnitPassGood;
import com.threerings.bang.store.data.Good;

import static com.threerings.bang.Log.log;

/**
 * Enumerates the various goods that can be rented by a gang for its members and associates them
 * with providers that are used to actually create and deliver the goods when purchased.
 */
@Singleton
public class RentalGoodsCatalog
{
    public static class RentalItemProvider extends GangItemProvider {
        public RentalItemProvider (
                GangObject gang, Handle handle, boolean admin, RentalGood good, Object[] args)
            throws InvocationException
        {
            super(gang, handle, admin, good, args);
            _scripCost = good.getRentalScripCost(gang);
            _coinCost = good.getRentalCoinCost(gang);
        }

        protected String getHistoryLogKey () {
            return "m.rental_entry";
        }
    }

    /**
     * Creates a rental good catalog, loading up the various bits necessary to create articles of
     * clothing and accessories for players.
     */
    @Inject public RentalGoodsCatalog (AvatarLogic alogic)
    {
        _alogic = alogic;

        // register articles
        ProviderFactory pf = new ArticleProviderFactory();
        for (ArticleCatalog.Article article : _alogic.getArticleCatalog().getArticles()) {
            if (article.hasExpired(System.currentTimeMillis())) {
                continue;
            }
            ArticleGood good = new ArticleGood(
                    article.townId + "/" + article.name, article.townId, article.scrip,
                    article.coins, article.qualifier, article.start, article.stop);
            registerGood(good, pf);
        }

        // the remained of the goods can generate their own items
        pf = new ItemProviderFactory();

        // register our unit passes
        UnitConfig[] units =
            UnitConfig.getTownUnits(BangCodes.TOWN_IDS[BangCodes.TOWN_IDS.length - 1]);
        for (int ii = 0; ii < units.length; ii++) {
            UnitConfig uc = units[ii];
            if (uc.badgeCode != 0 && uc.scripCost > 0) {
                UnitPassGood good = new UnitPassGood(
                        uc.type, uc.getTownId(), uc.scripCost, uc.coinCost);
                registerGood(good, pf);
            }
        }

        // free tickets!!
        int townIdx = BangUtil.getTownIndex(BangCodes.INDIAN_POST);
        TicketGood good = new TicketGood(BangCodes.INDIAN_POST, StationCodes.TICKET_SCRIP[townIdx],
                StationCodes.TICKET_COINS[townIdx]);
        registerGood(good, pf);
    }

    /**
     * Returns an array containing all available goods.
     */
    public RentalGood[] getGoods ()
    {
        return _goods.toArray(new RentalGood[_goods.size()]);
    }

    /**
     * Requests that a {@link GangGoodProvider} be created to provide the specified good to the
     * specified gang. Resturns null if no provider is registered for the good in question.
     */
    public GangGoodProvider getProvider (GangObject gang, Handle handle, boolean admin, 
                                         RentalGood good, Object[] args)
        throws InvocationException
    {
        ProviderFactory factory = _providers.get(good);
        if (factory != null) {
            return factory.createProvider(gang, handle, admin, good, args);
        }
        return null;
    }

    /**
     * Registers a RentalGood -> ProviderFactory mapping.
     */
    protected void registerGood (Good good, ProviderFactory factory)
    {
        RentalGood rgood = new RentalGood(good);
        _providers.put(rgood, factory);
        _goods.add(rgood);
    }

    /**
     * Returns the expiry date for newly rented items.
     */
    protected long getExpiryDate ()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, RENTAL_DAYS);
        return cal.getTimeInMillis();
    }

    /** Used to create a {@link Provider} for a particular {@link RentalGood}. */
    protected abstract class ProviderFactory {
        public abstract GangGoodProvider createProvider (
                GangObject gang, Handle handle, boolean admin, RentalGood good, Object[] args)
            throws InvocationException;
    }

    /** Used for generic items. */
    protected class ItemProviderFactory extends ProviderFactory {
        public GangGoodProvider createProvider (
                GangObject gang, Handle handle, boolean admin, RentalGood good, Object[] args)
            throws InvocationException
        {
            return new RentalItemProvider(gang, handle, admin, good, args) {
                protected Item createItem () throws InvocationException {
                    Item item = super.createItem();
                    item.setGangOwned(true);
                    item.setExpires(getExpiryDate());
                    return item;
                }
            };
        }
    }

    /** Used for {@link ArticleGood}s. */
    protected class ArticleProviderFactory extends ProviderFactory {
        public GangGoodProvider createProvider (
                GangObject gang, Handle handle, boolean admin, RentalGood good, Object[] args)
            throws InvocationException
        {
            return new RentalItemProvider(gang, handle, admin, good, args) {
                protected Item createItem () throws InvocationException {
                    ArticleCatalog.Article article =
                        _alogic.getArticleCatalog().getArticle(_good.getType());
                    if (article == null) {
                        log.warning("Requested to create article for unknown catalog entry",
                                    "who", _user.who(), "good", _good);
                        throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
                    }
                    // our arguments are colorization ids
                    int zations = AvatarLogic.composeZations(
                            toInt(_args[0]), toInt(_args[1]), toInt(_args[2]));
                    Item item = _alogic.createArticle(_gang.gangId, article, zations);
                    if (item == null) {
                        throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
                    }
                    item.setGangOwned(true);
                    item.setExpires(getExpiryDate());
                    return item;
                }
            };
        }
    }

    /**
     * Helpy helper function.
     */
    protected static int toInt (Object arg)
    {
        return (arg == null) ? 0 : (Integer)arg;
    }

    protected AvatarLogic _alogic;

    /** All available gang goods. */
    protected ArrayList<RentalGood> _goods = new ArrayList<RentalGood>();

    /** Contains mappings from {@link RentalGood} to {@link ProviderFactory} for all salable
     * goods. */
    protected HashMap<RentalGood, ProviderFactory> _providers =
        new HashMap<RentalGood, ProviderFactory>();

    /** The rental period in days. */
    protected static final int RENTAL_DAYS = 30;
}
