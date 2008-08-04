//
// $Id$

package com.threerings.bang.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.QuickSort;
import com.samskivert.util.ResultListener;

import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;

import com.threerings.coin.data.CoinExOfferInfo;
import com.threerings.coin.server.CoinExOffer;
import com.threerings.coin.server.CoinExchangeManager;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.ConsolidatedOffer;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.persist.PlayerRecord;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.server.GangPeerProvider;

import static com.threerings.bang.Log.log;

/**
 * Customizes the standard coin exchange for Bang! Howdy.
 */
public class BangCoinExchangeManager extends CoinExchangeManager
{
    /** Entities that will publish the coin exchange offers should implement this interface and
     * register themselves with the coin exchange manager to be informed when they should reread
     * and updated their list of published offers. */
    public interface OfferPublisher
    {
        /**
         * Called when the published offeers should be updated.
         *
         * @param buys if non-null, the latest consolidated buy offers.
         * @param sells if non-null, the latest consolidated sell offers.
         * @param lastPrice if not -1 the last trade price should be updated.
         */
        public void updateOffers (
                ConsolidatedOffer[] buys, ConsolidatedOffer[] sells, int lastPrice);

        /**
         * Called when an coin exchange offer has been modified.
         *
         * @param offerId the offerId of the modified coin exchange offer
         */
        public void offerModified (int offerId);

        /**
         * Called when coin exchange offers have been destroyed.
         *
         * @param offerIds an int array of destroyed coin exchange offerIds
         */
        public void offersDestroyed (int[] offerIds);
    }

    /**
     * Creates the coin exchange manager and its associated repository.
     */
    public BangCoinExchangeManager (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, ServerConfig.nodename);
    }

    /**
     * Prepares the coin exchange manager for operation.
     */
    public void init ()
        throws PersistenceException
    {
        init(BangServer.coinmgr, BangServer.invoker, BangCoinManager.coinlog,
             BangCodes.COINEX_OFFERS_SHOWN);

        // schedule an interval to clean out old offers periodically
        new Interval(BangServer.omgr) {
            public void expired () {
                periodicDeleteOffers();
            }
        }.schedule(PERIODIC_DELETE_START_INTERVAL, PERIODIC_DELETE_REPEAT_INTERVAL);
    }

    /**
     * Registers an offer publisher with the exchange manager.
     */
    public void registerPublisher (OfferPublisher publisher)
    {
        _publishers.add(publisher);

        // trigger a full published info update; ideally we'd only inform the newly registered
        // publisher, but there's only really ever one publisher anyway, so it's a wash
        updatePublishedInfo(true, true, _lastPrice);
    }

    /**
     * De-registers an offer publisher with the exchange manager.
     */
    public void removePublisher (OfferPublisher publisher)
    {
        _publishers.remove(publisher);
    }

    /**
     * Returns a two element array containing the outstanding buy and sell offers (in that order)
     * for the specified player. The returned arrays may be zero length but will not be null.
     */
    public CoinExOfferInfo[][] getPlayerOffers (PlayerObject player)
    {
        return new CoinExOfferInfo[][] {
            getPlayerOffers(player.username.toString(), _bids),
            getPlayerOffers(player.username.toString(), _asks),
        };
    }

    @Override // documentation inherited
    protected byte getFee ()
    {
        // birds do it, bees do it, bank that love the fees do it
        return (byte)2;
    }

    @Override // documentation inherited
    protected void updatePublishedInfo (boolean buy, boolean sell, int lastPrice)
    {
        ConsolidatedOffer[] buys = null;
        if (buy) {
            buys = summarizeOffers(_bids);
            QuickSort.rsort(buys);
        }
        ConsolidatedOffer[] sells = null;
        if (sell) {
            sells = summarizeOffers(_asks);
            QuickSort.sort(sells, _revcmp);
        }
        for (OfferPublisher publisher : _publishers) {
            publisher.updateOffers(buys, sells, lastPrice);
        }
    }

    @Override // documentation inherited
    protected void offersDestroyed (List<CoinExOffer> destroyed)
    {
        int[] offerIds = new int[destroyed.size()];
        for (int ii = 0; ii < offerIds.length; ii++) {
            offerIds[ii] = destroyed.get(ii).offerId;
        }
        for (OfferPublisher publisher : _publishers) {
            publisher.offersDestroyed(offerIds);
        }
    }

    @Override // documentation inherited
    protected void offerModified (CoinExOffer modified)
    {
        for (OfferPublisher publisher : _publishers) {
            publisher.offerModified(modified.offerId);
        }
    }

    @Override // documentation inherited
    protected void updateUserCoins (String gameName, String accountName)
    {
        if (accountName.startsWith("{")) {
            try {
                getGangPeerProvider(accountName).updateCoins(null);
            } catch (InvocationException ie) {
                log.warning("Failed to load gang to update coins. [ie=" + ie + "].");
            }
            return;
        }
        PlayerObject player = BangServer.locator.lookupByAccountName(new Name(accountName));
        if (player != null) {
            BangServer.coinmgr.updateCoinCount(player);
        }
    }

    @Override // documentation inherited
    protected void distributeCurrency (final CoinExOffer info, final int currency, final String msg)
    {
        // see if we're dealing with a gang
        if (info.accountName.indexOf("{") > -1) {
            try {
                getGangPeerProvider(info.accountName).grantScrip(null, currency);
            } catch (InvocationException ie) {
                log.warning("Failed to load gang for distributeCurrency. [ie=" + ie + "].");
            }
            return;
        }
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    BangServer.playrepo.grantScrip(info.accountName, currency);
                    return true;
                } catch (PersistenceException pe) {
                    log.warning("Failed to grant scrip to player " + "[offer=" + info +
                            ", amount=" + currency + ", type=" + msg + "].", pe);
                    return false;
                }
            }

            public void handleResult () {
                PlayerObject player =
                    BangServer.locator.lookupByAccountName(new Name(info.accountName));
                if (player != null) {
                    player.setScrip(player.scrip + currency);
                }
            }
        });
    }

    @Override // documentation inherited
    protected void reserveCurrency (Object user, final int cost, final ResultListener<Void> listener)
    {
        if (user instanceof GangObject) {
            reserveGangCurrency((GangObject)user, cost, listener);
            return;
        }

        // make sure they have the necessary currency to begin with
        final PlayerObject player = (PlayerObject)user;
        if (player.scrip < cost) {
            listener.requestFailed(new InvocationException(BangCodes.E_INSUFFICIENT_FUNDS));
            return;
        }

        // update their player object to indicate that it is spent
        player.setScrip(player.scrip - cost);

        // persist this expenditure to the database
        _invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    BangServer.playrepo.spendScrip(player.playerId, cost);
                } catch (PersistenceException pe) {
                    _error = pe;
                }
                return true;
            }

            public void handleResult () {
                if (_error == null) {
                    listener.requestCompleted(null);
                } else {
                    // return the scrip to the player object before failing
                    player.setScrip(player.scrip + cost);
                    listener.requestFailed(_error);
                }
            }

            protected PersistenceException _error;
        });
    }

    /**
     * Special handling for gang exchange offers.
     */
    protected void reserveGangCurrency (
        GangObject gang, final int cost, final ResultListener<Void> listener)
    {
        if (gang.scrip < cost) {
            listener.requestFailed(new InvocationException(BangCodes.E_INSUFFICIENT_FUNDS));
            return;
        }
        try {
            BangServer.gangmgr.requireGangPeerProvider(gang.gangId).reserveScrip(
                null, cost, new InvocationService.ConfirmListener() {
                    public void requestProcessed () {
                        listener.requestCompleted(null);
                    }
                    public void requestFailed (String cause) {
                        listener.requestFailed(new InvocationException(cause));
                    }
                });
        } catch (InvocationException ie) {
            log.warning("Unable to reserve gang currency! [ie=" + ie + "].");
            listener.requestFailed(ie);
        }
    }

    @Override // documentation inherited
    protected void tradeCompleted (
            int price, int vol, String seller, final String buyer, String buyerGame)
    {
        super.tradeCompleted(price, vol, seller, buyer, buyerGame);
        if (buyer.indexOf("{") > -1) {
            try {
                getGangPeerProvider(buyer).tradeCompleted(null, price, vol, buyerGame);
            } catch (InvocationException ie) {
                log.warning("Failed to load gang! [cause=" + ie + "].");
            }
            return;
        }
        PlayerObject player = BangServer.locator.lookupByAccountName(new Name(buyer));
        if (player != null) {
            _audit.log("bought_excoins " + player.playerId);
            return;
        }

        // if they're not online, we'll need to load them from the database
        _invoker.postUnit(new Invoker.Unit("tradeCompleted") {
            public boolean invoke () {
                try {
                    _user = BangServer.playrepo.loadPlayer(buyer);
                    return _user != null;
                } catch (PersistenceException pe) {
                    log.warning("Failed to load user! [cause=" + pe + "].");
                }
                return false;
            }

            public void handleResult () {
                _audit.log("bought_excoins " + _user.playerId);
            }

            protected PlayerRecord _user;
        });
    }

    /**
     * Helper function for getting a GangHandler from it's account name.
     */
    protected GangPeerProvider getGangPeerProvider (String accountName)
        throws InvocationException
    {
        Handle gangName = new Handle(accountName.substring(1, accountName.length() - 1));
        return BangServer.gangmgr.requireGangPeerProvider(gangName);
    }

    /**
     * Helper function for {@link #updatePublishedInfo}.
     */
    protected ConsolidatedOffer[] summarizeOffers (List offers)
    {
        ArrayList<ConsolidatedOffer> list = new ArrayList<ConsolidatedOffer>();
        for (int ii = 0, nn = offers.size(); ii < nn; ii++) {
            // start with an offer containing this info
            CoinExOffer offer = (CoinExOffer)offers.get(ii);
            ConsolidatedOffer published = new ConsolidatedOffer();
            published.price = offer.price;
            published.volume = offer.volume;

            // consolidate all subsequent offers at the same price
            for (int jj = ii + 1; jj < nn; jj++) {
                offer = (CoinExOffer) offers.get(jj);
                if (offer.price == published.price) {
                    published.volume += offer.volume;
                    ii++;
                } else {
                    break;
                }
            }

            list.add(published);
            if (list.size() == _offersShown) {
                break;
            }
        }
        return list.toArray(new ConsolidatedOffer[list.size()]);
    }

    /**
     * Helper function for {@link #getPlayerOffers(PlayerObject)}.
     */
    protected CoinExOfferInfo[] getPlayerOffers (String accountName, List offers)
    {
        ArrayList<CoinExOfferInfo> list = new ArrayList<CoinExOfferInfo>();
        for (int ii = 0, nn = offers.size(); ii < nn; ii++) {
            CoinExOffer offer = (CoinExOffer)offers.get(ii);
            if (offer.accountName.equals(accountName)) {
                list.add(createInfo(offer));
            }
        }
        return list.toArray(new CoinExOfferInfo[list.size()]);
    }

    protected List<OfferPublisher> _publishers = new ArrayList<OfferPublisher>();

    /** Used to reverse sort our offers. */
    protected Comparator<ConsolidatedOffer> _revcmp = new Comparator<ConsolidatedOffer>() {
        public int compare (ConsolidatedOffer o1, ConsolidatedOffer o2) {
            return o1.price - o2.price;
        }
    };
}
