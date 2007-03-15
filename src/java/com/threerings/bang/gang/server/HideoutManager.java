//
// $Id$

package com.threerings.bang.gang.server;

import java.util.ArrayList;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.BangNodeObject;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.avatar.server.BarberManager;

import com.threerings.bang.game.data.BangConfig;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.server.Match;
import com.threerings.bang.saloon.server.MatchHostManager;

import com.threerings.bang.store.data.Good;

import com.threerings.bang.gang.client.HideoutService;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangEntry;
import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutMarshaller;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.gang.data.TopRankedGangList;

import static com.threerings.bang.Log.log;

/**
 * Provides hideout-related services.
 */
public class HideoutManager extends MatchHostManager
    implements GangCodes, HideoutCodes, HideoutProvider
{
    /**
     * Adds an entry to the Hideout's list of gangs and broadcasts the addition to the server's
     * peers (if any).
     */
    public void addGang (GangEntry entry)
    {
        addGangLocal(entry);
        if (BangServer.peermgr != null) {
            ((BangNodeObject)BangServer.peermgr.getNodeObject()).setAddedGang(entry);
        }
    }

    /**
     * Removes an entry from the Hideout's list of gangs and broadcasts the removal to the server's
     * peers (if any).
     */
    public void removeGang (Handle name)
    {
        removeGangLocal(name);
        if (BangServer.peermgr != null) {
            ((BangNodeObject)BangServer.peermgr.getNodeObject()).setRemovedGang(name);
        }
    }

    /**
     * Adds an entry to the Hideout's list of gangs on this server only.
     */
    public void addGangLocal (GangEntry entry)
    {
        if (_hobj != null) {
            _hobj.addToGangs(entry);
        }
    }

    /**
     * Removes an entry from the Hideout's list on this server only.
     */
    public void removeGangLocal (Handle name)
    {
        if (_hobj != null) {
            _hobj.removeFromGangs(name);
        }
    }

    /**
     * Attempts to create a {@link GangGoodProvider} to purchase a good for the specified
     * gang.
     */
    public GangGoodProvider getGoodProvider (
        GangHandler gang, boolean admin, String type, Object[] args)
        throws InvocationException
    {
        // make sure we sell the good in question
        GangGood good = (GangGood)_hobj.goods.get(type);
        if (good == null) {
            log.warning("Requested to buy unknown good [gang=" + gang +
                        ", type=" + type + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // validate that the client can buy this good
        if (!good.isAvailable(gang.getGangObject())) {
            log.warning("Requested to buy unavailable good [gang=" + gang +
                        ", good=" + good + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // create the appropriate provider and return it
        GangGoodProvider provider = _goods.getProvider(gang.getGangObject(), admin, good, args);
        if (provider == null) {
            log.warning("Unable to find provider for good [gang=" + gang +
                        ", good=" + good + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        return provider;
    }

    // documentation inherited from interface HideoutProvider
    public void formGang (ClientObject caller, Handle root, String suffix,
                          final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're not already in a gang
        final PlayerObject user = requireShopEnabled(caller);
        if (user.gangId > 0) {
            log.warning("Player tried to form a gang when already in one " +
                "[who=" + user.who() + ", gangId=" + user.gangId + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure the suffix is in the approved set
        if (!NameFactory.getCreator().getGangSuffixes().contains(suffix)) {
            log.warning("Tried to form gang with invalid suffix [who=" +
                user.who() + ", suffix=" + suffix + "].");
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
    public void leaveGang (ClientObject caller, final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it off to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).removeFromGang(
            null, null, user.handle, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void setStatement (ClientObject caller, String statement, String url,
                              final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // make sure the entries are valid
        if (statement == null || statement.length() > MAX_STATEMENT_LENGTH) {
            log.warning("Invalid statement [who=" + user.who() + ", statement=" +
                statement + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        if (url == null || url.length() > MAX_URL_LENGTH) {
            log.warning("Invalid URL [who=" + user.who() + ", url=" + url + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // pass it off to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).setStatement(
            null, user.handle, statement, url, listener);
    }

    // documentation inherited from HideoutProvider
    public void setBuckle (ClientObject caller, BucklePart[] parts,
                           HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it off to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).setBuckle(
            null, user.handle, parts, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void addToCoffers (ClientObject caller, final int scrip, final int coins,
                              final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // make sure they can donate; we return a user-friendly message if not, even though they
        // shouldn't see the option, because their clock may not match up with ours
        if (!user.canDonate()) {
            throw new InvocationException("e.too_soon");
        }

        // make sure the amounts are positive and that at least one is nonzero
        if (scrip < 0 || coins < 0 || scrip + coins == 0) {
            log.warning("Player tried to donate invalid amounts [who=" +
                user.who() + ", scrip=" + scrip + ", coins=" + coins + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // pass it off to the gang handler
        BangServer.gangmgr.requireGang(user.gangId).addToCoffers(
            user, scrip, coins, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void expelMember (ClientObject caller, final Handle handle,
                             final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it off to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).removeFromGang(
            null, user.handle, handle, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void changeMemberRank (ClientObject caller, Handle handle, byte rank,
                                  HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // make sure it's a valid rank
        if (rank < 0 || rank >= RANK_COUNT) {
            log.warning("Tried to change member to invalid rank [who=" + user.who() +
                ", target=" + handle + ", rank=" + rank + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).changeMemberRank(
            null, user.handle, handle, rank, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void getHistoryEntries (ClientObject caller, int offset,
                                   HideoutService.ResultListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // make sure the offset is valid
        if (offset < 0) {
            log.warning("Invalid history entry offset [who=" + user.who() + ", offset=" + offset +
                "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // pass it off to the gang handler.  we ask for one more than we display on a page in
        // order to find out if there are any on the previous page
        BangServer.gangmgr.requireGang(user.gangId).getHistoryEntries(
            offset, HISTORY_PAGE_ENTRIES + 1, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void getOutfitQuote (ClientObject caller, OutfitArticle[] outfit,
                                HideoutService.ResultListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).processOutfits(
            null, user.handle, outfit, false, user.tokens.isAdmin(), listener);
    }

    // documentation inherited from interface HideoutProvider
    public void buyOutfits (ClientObject caller, OutfitArticle[] outfit,
                            HideoutService.ResultListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).processOutfits(
            null, user.handle, outfit, true, user.tokens.isAdmin(), listener);
    }

    // documentation inherited from interface HideoutProvider
    public void buyGangGood (ClientObject caller, String type, Object[] args,
                             HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they have access
        PlayerObject user = requireShopEnabled(caller);

        // pass it on to the gang handler
        BangServer.gangmgr.requireGangPeerProvider(user.gangId).buyGangGood(
            null, user.handle, type, args, user.tokens.isAdmin(), listener);
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
    protected void didInit ()
    {
        super.didInit();

        // create our goods catalog
        _goods = new GangGoodsCatalog(BangServer.alogic);
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _hobj = (HideoutObject)_plobj;
        _hobj.setService((HideoutMarshaller)
                         BangServer.invmgr.registerDispatcher(new HideoutDispatcher(this)));
        _hobj.setGoods(new DSet<Good>(_goods.getGoods()));

        // load up the gangs for the directory
        BangServer.gangmgr.loadGangs(new ResultListener<ArrayList<GangEntry>>() {
            public void requestCompleted (ArrayList<GangEntry> result) {
                _hobj.setGangs(new DSet<GangEntry>(result.iterator()));
            }
            public void requestFailed (Exception cause) {
                log.warning("Failed to load gang list [error=" + cause + "].");
            }
        });

        // start up our top-ranked list refresher interval
        _rankval = new Interval(BangServer.omgr) {
            public void expired () {
                refreshTopRanked();
            }
        };
        _rankval.schedule(1000L, RANK_REFRESH_INTERVAL);
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

    @Override // from MatchHostManager
    protected void checkCriterion (Criterion criterion)
    {
        // we never allow ais
        criterion.allowAIs = 1;
        super.checkCriterion(criterion);
    }

    @Override // from MatchHostManager
    protected Match createMatch (PlayerObject user, Criterion criterion)
    {
        return new Match(user, criterion) {
            public boolean join (PlayerObject player, Criterion criterion) {
                // ranked games require every player to be in a different gang (until we have team
                // games); unranked games require every player to be in the same gang
                boolean ranked = _criterion.getDesiredRankedness();
                for (PlayerObject oplayer : players) {
                    if (oplayer != null && ranked == (player.gangId == oplayer.gangId)) {
                        return false;
                    }
                }
                return super.join(player, criterion);
            }
            public BangConfig createConfig () {
                // grant notoriety for rated (competition) games
                BangConfig config = super.createConfig();
                if (config.rated) {
                    config.grantNotoriety = true;
                }
                return config;
            }
        };
    }

    protected void refreshTopRanked ()
    {
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _lists = new ArrayList<TopRankedGangList>();
                    _lists.add(BangServer.gangrepo.loadTopRankedByNotoriety(TOP_RANKED_LIST_SIZE));
                    return true;

                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to load top-ranked gangs.", pe);
                    return false;
                }
            }

            public void handleResult () {
                // make sure we weren't shutdown while we were off invoking
                if (!_hobj.isActive()) {
                    return;
                }
                _hobj.startTransaction();
                try {
                    for (TopRankedGangList list : _lists) {
                        if (_hobj.topRanked.containsKey(list.criterion)) {
                            _hobj.updateTopRanked(list);
                        } else {
                            _hobj.addToTopRanked(list);
                        }
                    }
                } finally {
                    _hobj.commitTransaction();
                }
            }

            protected ArrayList<TopRankedGangList> _lists;
        });
    }

    protected GangGoodsCatalog _goods;
    protected HideoutObject _hobj;
    protected Interval _rankval;

    /** The frequency with which we update the top-ranked gang lists. */
    protected static final long RANK_REFRESH_INTERVAL = 60 * 60 * 1000L;

    /** The size of the top-ranked gang lists. */
    protected static final int TOP_RANKED_LIST_SIZE = 10;
}
