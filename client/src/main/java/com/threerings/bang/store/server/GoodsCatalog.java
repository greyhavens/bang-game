//
// $Id$

package com.threerings.bang.store.server;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.data.scenario.CattleRustlingInfo;
import com.threerings.bang.game.data.scenario.ClaimJumpingInfo;
import com.threerings.bang.game.data.scenario.ForestGuardiansInfo;
import com.threerings.bang.game.data.scenario.GoldRushInfo;
import com.threerings.bang.game.data.scenario.TotemBuildingInfo;
import com.threerings.bang.game.data.scenario.WendigoAttackInfo;

import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Star;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.CardPackGood;
import com.threerings.bang.store.data.CardTripletGood;
import com.threerings.bang.store.data.ExchangePassGood;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.PurseGood;
import com.threerings.bang.store.data.SongGood;
import com.threerings.bang.store.data.StarGood;
import com.threerings.bang.store.data.UnitPassGood;

import static com.threerings.bang.Log.log;

/**
 * Enumerates the various goods that can be purchased from the General Shop and associates them
 * with providers that are used to actually create and deliver the goods when purchased.
 */
@Singleton
public class GoodsCatalog
{
    /**
     * Creates a goods catalog, loading up the various bits necessary to create articles of
     * clothing and accessories for avatars.
     */
    @Inject public GoodsCatalog (AvatarLogic alogic)
    {
        _alogic = alogic;

        // register our packs of cards
        ProviderFactory pf = new CardProviderFactory();
        for (int ii = 0; ii < PACK_PRICES.length; ii += 3) {
            for (int townIdx = 0; townIdx < BangCodes.TOWN_IDS.length; townIdx++) {
                registerGood(new CardPackGood(PACK_PRICES[ii], BangCodes.TOWN_IDS[townIdx],
                                 PACK_PRICES[ii+1], PACK_PRICES[ii+2]), pf);
            }
        }
        for (Card card : Card.getCards()) {
            // not all cards are for sale individually
            if (card.getScripCost() <= 0) {
                continue;
            }
            Good good = new CardTripletGood(
                card.getType(), card.getTownId(), card.getScripCost(), 0, card.getQualifier());
            registerGood(good, pf);
        }

        // use the avatar article catalog to create goods for all avatar articles
        pf = new ArticleProviderFactory();
        for (ArticleCatalog.Article article : _alogic.getArticleCatalog().getArticles()) {
            if (article.hasExpired(System.currentTimeMillis())) {
                continue;
            }
            ArticleGood good = new ArticleGood(
                    article.townId + "/" + article.name, article.townId, article.scrip,
                    article.coins, article.qualifier, article.start, article.stop);
            registerGood(good, pf);
        }

        // the remainder of the goods can generate their own items
        pf = new ItemProviderFactory();

        // register our purses and exchange pass
        for (int townIdx = 0; townIdx < BangCodes.TOWN_IDS.length; townIdx++) {
            registerGood(new PurseGood(townIdx), pf);
            if (DeploymentConfig.usesCoins()) {
                registerGood(new ExchangePassGood(BangCodes.TOWN_IDS[townIdx]), pf);
            }
        }

        // register our unit passes
        UnitConfig[] units = UnitConfig.getTownUnits(ServerConfig.townId);
        for (int ii = 0; ii < units.length; ii++) {
            UnitConfig uc = units[ii];
            if (uc.badgeCode != 0 && uc.scripCost > 0) {
                UnitPassGood good = new UnitPassGood(
                        uc.type, uc.getTownId(), uc.scripCost, uc.coinCost);
                registerGood(good, pf);
            }
        }

        // register our deputy sheriff's stars
        for (int townIdx = 0; townIdx < BangCodes.TOWN_IDS.length; townIdx++) {
            for (Star.Difficulty diff : Star.Difficulty.values()) {
                if (diff == Star.Difficulty.EASY) { // no easy star
                    continue;
                }
                registerGood(new StarGood(townIdx, diff), pf);
            }
        }

        // register our music
        registerGood(new SongGood(BangCodes.FRONTIER_TOWN, BangCodes.FRONTIER_TOWN), pf);
        registerGood(new SongGood(ClaimJumpingInfo.IDENT, BangCodes.FRONTIER_TOWN), pf);
        registerGood(new SongGood(CattleRustlingInfo.IDENT, BangCodes.FRONTIER_TOWN), pf);
        registerGood(new SongGood(GoldRushInfo.IDENT, BangCodes.FRONTIER_TOWN), pf);

        registerGood(new SongGood(BangCodes.INDIAN_POST, BangCodes.INDIAN_POST), pf);
        registerGood(new SongGood(TotemBuildingInfo.IDENT, BangCodes.INDIAN_POST), pf);
        registerGood(new SongGood(WendigoAttackInfo.IDENT, BangCodes.INDIAN_POST), pf);
        registerGood(new SongGood(ForestGuardiansInfo.IDENT, BangCodes.INDIAN_POST), pf);
    }

    /**
     * Returns the goods that are available in the town in question.
     */
    public ArrayList<Good> getGoods (String townId)
    {
        ArrayList<Good> goods = new ArrayList<Good>();
        //goods.addAll(_tgoods.get("")); // global goods
        goods.addAll(_tgoods.get(townId)); // goods for sale in this town
        return goods;
    }

    /**
     * Requests that a {@link Provider} be created to provide the specified good to the specified
     * user. Returns null if no provider is registered for the good in question.
     */
    public Provider getProvider (PlayerObject user, Good good, Object[] args)
        throws InvocationException
    {
        ProviderFactory factory = _providers.get(good);
        if (factory != null) {
            return factory.createProvider(user, good, args);
        }
        return null;
    }

    /**
     * Registers a Good -> ProviderFactory mapping for the specified town.
     */
    protected void registerGood (Good good, ProviderFactory factory)
    {
        _providers.put(good, factory);
        ArrayList<Good> goods = _tgoods.get(good.getTownId());
        if (goods == null) {
            _tgoods.put(good.getTownId(), goods = new ArrayList<Good>());
        }
        goods.add(good);
    }

    /**
     * Helpy helper function.
     */
    protected static int toInt (Object arg)
    {
        return (arg == null) ? 0 : (Integer)arg;
    }

    /** Used to create a {@link Provider} for a particular {@link Good}. */
    protected abstract class ProviderFactory {
        public abstract Provider createProvider (PlayerObject user, Good good, Object[] args)
            throws InvocationException;
    }

    /** Used for {@link CardPackGood}s and {@link CardTripletGood}s. */
    protected class CardProviderFactory extends ProviderFactory {
        public Provider createProvider (PlayerObject user, Good good, Object[] args)
            throws InvocationException
        {
            if (good instanceof CardPackGood) {
                return new CardPackProvider(user, good);
            } else {
                return new CardTripletProvider(user, good);
            }
        }
    }

    /** Used for {@link ArticleGood}s. */
    protected class ArticleProviderFactory extends ProviderFactory {
        public Provider createProvider (PlayerObject user, Good good, Object[] args)
            throws InvocationException
        {
            return new ItemProvider(user, good, args) {
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
                    Item item = _alogic.createArticle(_user.playerId, article, zations);
                    if (item == null) {
                        throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
                    }
                    return item;
                }
            };
        }
    }

    /** Used for goods that can handle creation themselves. */
    protected class ItemProviderFactory extends ProviderFactory {
        public Provider createProvider (PlayerObject user, Good good, Object[] args)
            throws InvocationException
        {
            return new ItemProvider(user, good, args);
        }
    }

    /** Handles all of our avatar related bits. */
    protected AvatarLogic _alogic;

    /** A mapping from town id to a list of goods available in that town. */
    protected HashMap<String,ArrayList<Good>> _tgoods = new HashMap<String,ArrayList<Good>>();

    /** Contains mappings from {@link Good} to {@link ProviderFactory} for all salable goods. */
    protected HashMap<Good,ProviderFactory> _providers = new HashMap<Good,ProviderFactory>();

    /** Quantity, scrip cost and coin cost for our packs of cards. */
    protected static final int[] PACK_PRICES = {
        13, 13*10, 1,
        52, 52*10, 3,
    };
}
