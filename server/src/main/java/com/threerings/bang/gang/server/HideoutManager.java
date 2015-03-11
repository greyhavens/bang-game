//
// $Id$

package com.threerings.bang.gang.server;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangNodeObject;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.ConsolidatedOffer;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangPeerManager;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.avatar.server.BarberManager;
import com.threerings.bang.chat.server.BangChatManager;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.server.MatchHostManager;

import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.Good;

import com.threerings.bang.gang.client.HideoutService;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangEntry;
import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutMarshaller;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.gang.data.RentalGood;
import com.threerings.bang.gang.data.TopRankedGangList;
import com.threerings.bang.gang.server.persist.GangRepository;

import static com.threerings.bang.Log.log;

/**
 * Provides hideout-related services.
 */
@Singleton
public class HideoutManager extends MatchHostManager
    implements GangCodes, HideoutCodes, HideoutProvider
{
    /**
     * Adds an entry to the Hideout's list of gangs or reactivates an existing gang and broadcasts
     * the activation to the server's peers (if any).
     */
    public void activateGang (Handle name)
    {
        activateGangLocal(name);
        if (_peermgr.isRunning()) {
            ((BangNodeObject)_peermgr.getNodeObject()).setActivatedGang(name);
        }
    }

    /**
     * Removes an entry from the Hideout's list of gangs and broadcasts the removal to the server's
     * peers (if any).
     */
    public void removeGang (Handle name)
    {
        removeGangLocal(name);
        if (_peermgr.isRunning()) {
            ((BangNodeObject)_peermgr.getNodeObject()).setRemovedGang(name);
        }
    }

    /**
     * (Re)activates a gang on this server only.
     */
    public void activateGangLocal (Handle name)
    {
        // if there's no entry, create one; otherwise, refresh the time
        GangEntry entry = _hobj.gangs.get(name);
        if (entry == null) {
            _hobj.addToGangs(new GangEntry(name));
        } else {
            entry.lastPlayed = System.currentTimeMillis();
        }
        // add this gang's name words to the chat whitelist
        _chatmgr.addWhitelistWords(name.toString());
    }

    /**
     * Removes an entry from the Hideout's list on this server only.
     */
    public void removeGangLocal (Handle name)
    {
        if (_hobj != null && _hobj.gangs.containsKey(name)) {
            _hobj.removeFromGangs(name);
        }
    }

    /**
     * Attempts to create a {@link GangGoodProvider} to purchase a good for the specified
     * gang.
     */
    public GangGoodProvider getGoodProvider (
        GangHandler gang, Handle handle, boolean admin, String type, Object[] args)
        throws InvocationException
    {
        // make sure we sell the good in question
        GangGood good = (GangGood)_hobj.goods.get(type);
        if (good == null) {
            log.warning("Requested to buy unknown good", "gang", gang, "handle", handle,
                        "type", type);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // validate that the client can buy this good
        if (!good.isAvailable(gang.getGangObject())) {
            log.warning("Requested to buy unavailable good", "gang", gang, "handle", handle,
                        "good", good);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // create the appropriate provider and return it
        GangGoodProvider provider = _goods.getProvider(
            gang.getGangObject(), handle, admin, good, args);
        if (provider == null) {
            log.warning("Unable to find provider for good", "gang", gang, "handle", handle,
                        "good", good);
            throw new InvocationException(INTERNAL_ERROR);
        }
        return provider;
    }

    /**
     * Attempts to create a {@link GangGoodProvider} to rent a good for the specified gang.
     */
    public GangGoodProvider getRentalGoodProvider (
        GangHandler gang, Handle handle, boolean admin, String type, Object[] args)
        throws InvocationException
    {
        // make sure we sell the good in question
        RentalGood good = (RentalGood)_hobj.rentalGoods.get(type);
        if (good == null) {
            log.warning("Requested to rent unknown good", "gang", gang, "handle", handle,
                        "type", type);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // validate that the client can rent this good
        if (!good.isAvailable(gang.getGangObject())) {
            log.warning("Requested to rent unavailable good", "gang", gang, "handle", handle,
                        "good", good);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // create the appropriate provider and return it
        GangGoodProvider provider = _rentalGoods.getProvider(
            gang.getGangObject(), handle, admin, good, args);
        if (provider == null) {
            log.warning("Unable to find provider for good", "gang", gang, "handle", handle,
                        "good", good);
            throw new InvocationException(INTERNAL_ERROR);
        }
        return provider;
    }

    /**
     * Returns a rental good that would create the provided item.
     */
    public RentalGood getRentalGood (Item item)
    {
        return _hobj.getRentalGood(item);
    }

    // documentation inherited from interface HideoutProvider
    public void formGang (PlayerObject caller, Handle root, String suffix,
                          HideoutService.ConfirmListener listener) throws InvocationException
    {
        final PlayerObject user = requireShopEnabled(caller);

        // make sure they're not already in a gang
        if (user.gangId > 0) {
            log.warning("Player tried to form a gang when already in one", "who", user.who(),
                        "gangId", user.gangId);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure they have the cowpoke badge
        if (!(user.tokens.isAdmin() || user.holdsBadge(Badge.Type.GAMES_PLAYED_2))) {
            throw new InvocationException("m.missing_badge");
        }

        // make sure the suffix is in the approved set
        if (!NameFactory.getCreator().getGangSuffixes().contains(suffix)) {
            log.warning("Tried to form gang with invalid suffix", "who", user.who(),
                        "suffix", suffix);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // validate the root using the same rules as the BarberManager
        BarberManager.validateHandle(user, root);

        // make sure the name isn't already in use
        Handle name = new Handle(root + " " + suffix);
        if (_hobj.gangs.containsKey(name)) {
            throw new InvocationException("m.duplicate_gang_name");
        }

        // form the name and start up the financial action
        BangServer.gangmgr.formGang(user, name, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void leaveGang (PlayerObject caller, HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it off to the gang handler
        BangServer.gangmgr.requireGang(user.gangId).leaveGang(user, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void setStatement (PlayerObject caller, String statement, String url,
                              HideoutService.ConfirmListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // make sure the entries are valid
        if (statement == null || statement.length() > MAX_STATEMENT_LENGTH) {
            log.warning("Invalid statement", "who", user.who(), "statement", statement);
            throw new InvocationException(INTERNAL_ERROR);
        }
        if (url == null || url.length() > MAX_URL_LENGTH) {
            log.warning("Invalid URL", "who", user.who(), "url", url);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // pass it off to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).setStatement(
            null, user.handle, statement, url, listener);
    }

    // documentation inherited from HideoutProvider
    public void setBuckle (PlayerObject caller, BucklePart[] parts,
                           HideoutService.ConfirmListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it off to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).setBuckle(
            null, user.handle, parts, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void addToCoffers (PlayerObject caller, final int scrip, final int coins,
                              HideoutService.ConfirmListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // make sure the amounts are positive and that at least one is nonzero
        if (scrip < 0 || coins < 0 || scrip + coins == 0) {
            log.warning("Player tried to donate invalid amounts", "who", user.who(),
                        "scrip", scrip, "coins", coins);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // pass it off to the gang handler
        BangServer.gangmgr.requireGang(user.gangId).addToCoffers(user, scrip, coins, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void expelMember (PlayerObject caller, final Handle handle,
                             HideoutService.ConfirmListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it off to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).removeFromGang(
            null, user.handle, handle, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void changeMemberRank (PlayerObject caller, Handle handle, byte rank,
                                  HideoutService.ConfirmListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // make sure it's a valid rank
        if (rank < 0 || rank >= RANK_COUNT) {
            log.warning("Tried to change member to invalid rank", "who", user.who(),
                        "target", handle, "rank", rank);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).changeMemberRank(
            null, user.handle, handle, rank, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void changeMemberTitle (PlayerObject caller, Handle handle, int title,
                                  HideoutService.ConfirmListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // make sure it's a valid rank
        if (title < 0 || title > TITLES_COUNT) {
            log.warning("Tried to change member to invalid title", "who", user.who(),
                        "target", handle, "title", title);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).changeMemberTitle(
            null, user.handle, handle, title, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void getHistoryEntries (PlayerObject caller, int offset, String filter,
                                   HideoutService.ResultListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // make sure the offset is valid
        if (offset < 0) {
            log.warning("Invalid history entry offset", "who", user.who(), "offset", offset);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // pass it off to the gang handler.  we ask for one more than we display on a page in
        // order to find out if there are any on the previous page
        BangServer.gangmgr.requireGang(user.gangId).getHistoryEntries(
            offset, HISTORY_PAGE_ENTRIES + 1, filter, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void getUpgradeQuote (PlayerObject caller, GangGood good,
                                 HideoutService.ResultListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).getUpgradeQuote(
                null, user.handle, good, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void getOutfitQuote (PlayerObject caller, OutfitArticle[] outfit,
                                HideoutService.ResultListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).processOutfits(
            null, user.handle, outfit, false, user.tokens.isAdmin(), listener);
    }

    // documentation inherited from interface HideoutProvider
    public void buyOutfits (PlayerObject caller, OutfitArticle[] outfit,
                            HideoutService.ResultListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).processOutfits(
            null, user.handle, outfit, true, user.tokens.isAdmin(), listener);
    }

    // documentation inherited from interface HideoutProvider
    public void buyGangGood (PlayerObject caller, String type, Object[] args,
                             HideoutService.ConfirmListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).buyGangGood(
            null, user.handle, type, args, user.tokens.isAdmin(), listener);
    }

    // documentation inherited from interface HideoutProvider
    public void rentGangGood (PlayerObject caller, String type, Object[] args,
                              HideoutService.ConfirmListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).rentGangGood(
            null, user.handle, type, args, user.tokens.isAdmin(), listener);
    }

    // documentation inherited from interface HideoutProvider
    public void renewGangItem (PlayerObject caller, int itemId,
                               HideoutService.ConfirmListener listener) throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).renewGangItem(
                null, user.handle, itemId, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void broadcastToMembers (PlayerObject caller, String message,
                                    HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // ignore empty messages
        if (StringUtil.isBlank(message)) {
            listener.requestProcessed();
            return;
        }

        // make sure the message is under the length limit
        if (message.length() > MAX_BROADCAST_LENGTH) {
            log.warning("Overlong broadcast message", "who", user.who(), "message", message);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).broadcastToMembers(
            null, user.handle, message, listener);
    }

    /**
     * Calculates the cost to upgrade rented gang items to a new weight class.
     */
    public int[] upgradeCost(GangObject gangobj, byte weightClass)
    {
        int[] cost = new int[2];
        int oldWeightClass = gangobj.getWeightClass();
        if (weightClass <= oldWeightClass) {
            return cost;
        }
        float rentDiff = RuntimeConfig.server.rentMultiplier[weightClass] -
            RuntimeConfig.server.rentMultiplier[oldWeightClass];
        float articleRentDiff = RuntimeConfig.server.articleRentMultiplier[weightClass] -
            RuntimeConfig.server.articleRentMultiplier[oldWeightClass];
        long now = System.currentTimeMillis();
        for (Item item : gangobj.inventory) {
            if (item.getExpires() != 0 && !item.isExpired(now)) {
                RentalGood rgood = _hobj.getRentalGood(item);
                if (rgood == null) {
                    continue;
                }
                Good good = rgood.getGood();
                float remaining = (float)(item.getExpires() - now) / RENTAL_PERIOD;
                if (good instanceof ArticleGood) {
                    cost[0] += Math.round(good.getCoinCost() * articleRentDiff * remaining);
                    cost[1] += Math.round(good.getScripCost() * articleRentDiff * remaining);
                } else {
                    cost[0] += Math.round(good.getCoinCost() * rentDiff * remaining);
                    cost[1] += Math.round(good.getScripCost() * rentDiff * remaining);
                }

            }
        }
        return cost;
    }

    @Override // from ShopManager
    protected String getIdent ()
    {
        return "hideout";
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new HideoutObject();
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _hobj = (HideoutObject)_plobj;
        _hobj.setService(BangServer.invmgr.registerProvider(this, HideoutMarshaller.class));
        _hobj.setGoods(new DSet<Good>(_goods.getGoods()));
        _hobj.setRentalGoods(new DSet<Good>(_rentalGoods.getGoods()));

        // load up the gangs for the directory
        BangServer.gangmgr.loadGangs(new ResultListener<List<GangEntry>>() {
            public void requestCompleted (List<GangEntry> result) {
                _hobj.setGangs(new DSet<GangEntry>(result.iterator()));
            }
            public void requestFailed (Exception cause) {
                log.warning("Failed to load gang list", "error", cause);
            }
        });

        // start up our interval to refresh the top-ranked list and purge inactive gangs
        _rankval = new Interval(BangServer.omgr) {
            public void expired () {
                refreshTopRanked();
                purgeInactiveGangs();
            }
        };
        _rankval.schedule(1000L, RANK_PURGE_INTERVAL);
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        super.didShutdown();

        // clear out our invocation service
        if (_hobj != null) {
            BangServer.invmgr.clearDispatcher(_hobj.service);
            _hobj = null;
        }

        // stop our top-ranked list refresher
        if (_rankval != null) {
            _rankval.cancel();
            _rankval = null;
        }
    }

    @Override // from PlaceManager
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);
        PlayerObject user = (PlayerObject)BangServer.omgr.getObject(bodyOid);
        if (user.gangId <= 0) {
            return;
        }
        try {
            BangOccupantInfo boi = (BangOccupantInfo)_occInfo.get(bodyOid);
            BangServer.gangmgr.requireGangPeerProvider(user.gangId).memberEnteredHideout(
                    null, user.handle, boi.avatar);
        } catch (InvocationException e) {
            // an exception will have been logged already
        }
    }

    @Override // from PlaceManager
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);
        PlayerObject user = (PlayerObject)BangServer.omgr.getObject(bodyOid);
        if (user.gangId <= 0) {
            return;
        }
        try {
            GangHandler handler = BangServer.gangmgr.requireGang(user.gangId);
            handler.bodyLeft(bodyOid);
            handler.getPeerProvider().memberLeftHideout(null, user.handle);
        } catch (InvocationException e) {
            // an exception will have been logged already
        }
    }

    @Override // documentation inherited
    protected void bodyUpdated (OccupantInfo info)
    {
        super.bodyUpdated(info);

        // if a player disconnects during the matchmaking phase, remove them
        // from their pending match
        if (info.status == OccupantInfo.DISCONNECTED) {
            PlayerObject user = (PlayerObject)BangServer.omgr.getObject(info.bodyOid);
            if (user.gangId <= 0) {
                return;
            }
            try {
                GangHandler handler = BangServer.gangmgr.requireGang(user.gangId);
                handler.bodyLeft(info.bodyOid);
            } catch (InvocationException e) {
                // an exception will have been logged already
            }
        }
    }

    @Override // from MatchHostManager
    protected void checkCriterion (Criterion criterion)
    {
        super.checkCriterion(criterion);

        criterion.gang = true;
    }

    protected void refreshTopRanked ()
    {
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _lists = new ArrayList<TopRankedGangList>();
                    for (byte ii = 0; ii < WEIGHT_CLASSES.length; ii++) {
                        TopRankedGangList list = _gangrepo.loadTopRankedByNotoriety(
                            ii, TOP_RANKED_LIST_SIZE);
                        if (list != null) {
                            _lists.add(list);
                        }
                    }
                    return true;

                } catch (PersistenceException pe) {
                    log.warning("Failed to load top-ranked gangs.", pe);
                    return false;
                }
            }

            public void handleResult () {
                // make sure we weren't shutdown while we were off invoking
                if (_hobj.isActive()) {
                    _hobj.setTopRanked(new DSet<TopRankedGangList>(_lists));
                }
            }

            protected List<TopRankedGangList> _lists;
        });
    }

    protected void purgeInactiveGangs ()
    {
        long cutoff = System.currentTimeMillis() - ACTIVITY_DELAY;
        List<GangEntry> removals = new ArrayList<GangEntry>();
        for (GangEntry entry : _hobj.gangs) {
            if (entry.lastPlayed < cutoff) {
                removals.add(entry);
            }
        }
        if (removals.isEmpty()) {
            return;
        }

        _hobj.startTransaction();
        try {
            for (GangEntry entry : removals) {
                _hobj.removeFromGangs(entry.name);
            }
        } finally {
            _hobj.commitTransaction();
        }
    }

    protected HideoutObject _hobj;
    protected Interval _rankval;

    // dependencies
    @Inject protected BangChatManager _chatmgr;
    @Inject protected BangPeerManager _peermgr;
    @Inject protected GangGoodsCatalog _goods;
    @Inject protected GangRepository _gangrepo;
    @Inject protected RentalGoodsCatalog _rentalGoods;

    /** The frequency with which we update the top-ranked gang lists and purge inactive gangs. */
    protected static final long RANK_PURGE_INTERVAL = 60 * 60 * 1000L;

    /** The size of the top-ranked gang lists. */
    protected static final int TOP_RANKED_LIST_SIZE = 10;

    /** A day in milliseconds. */
    protected static final long RENTAL_PERIOD = 30 * 24 * 60 * 60 * 1000L;
}
