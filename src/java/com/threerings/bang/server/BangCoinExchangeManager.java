//
// $Id$

package com.threerings.bang.server;

import java.util.ArrayList;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;

import com.threerings.util.Name;

import com.threerings.presents.server.InvocationException;

import com.threerings.coin.server.CoinExOffer;
import com.threerings.coin.server.CoinExchangeManager;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;

import static com.threerings.bang.Log.log;

/**
 * Customizes the standard coin exchange for Bang! Howdy.
 */
public class BangCoinExchangeManager extends CoinExchangeManager
{
    /** Entities that will publish the coin exchange offers should implement
     * this interface and register themselves with the coin exchange manager to
     * be informed when they should reread and updated their list of published
     * offers. */
    public interface OfferPublisher
    {
        /**
         * Called when the published offeers should be updated.
         *
         * @param buy if true, the buy offers should be updated.
         * @param sell if true, the sell offers should be updated.
         * @param lastPrice if not -1 the last trade price should be updated.
         */
        public void updateOffers (boolean buy, boolean sell, int lastPrice);
    }

    /** The number of offers of each type we publish in the coin exchange. */
    public static final int COINEX_OFFERS_SHOWN = 5;

    /**
     * Creates the coin exchange manager and its associated repository.
     */
    public BangCoinExchangeManager (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov);
    }

    /**
     * Prepares the coin exchange manager for operation.
     */
    public void init ()
        throws PersistenceException
    {
        init(BangServer.coinmgr, BangServer.invoker, BangCoinManager.coinlog,
             COINEX_OFFERS_SHOWN);
    }

    /**
     * Registers an offer publisher with the exchange manager.
     */
    public void registerPublisher (OfferPublisher publisher)
    {
        _publishers.add(publisher);
    }

    /**
     * De-registers an offer publisher with the exchange manager.
     */
    public void removePublisher (OfferPublisher publisher)
    {
        _publishers.remove(publisher);
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
        for (OfferPublisher publisher : _publishers) {
            publisher.updateOffers(buy, sell, lastPrice);
        }
    }

    @Override // documentation inherited
    protected void updateUserCoins (String gameName, String accountName)
    {
        PlayerObject player = (PlayerObject)BangServer.lookupBody(
            new Name(accountName));
        if (player != null) {
            BangServer.coinmgr.updateCoinCount(player);
        }
    }

    @Override // documentation inherited
    protected void distributeCurrency (
        final CoinExOffer info, final int currency, final String msg)
    {
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    BangServer.playrepo.grantScrip(info.accountName, currency);
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to grant scrip to player " +
                            "[offer=" + info + ", amount=" + currency +
                            ", type=" + msg + "].", pe);
                }
                return false;
            }
        });
    }

    @Override // documentation inherited
    protected void reserveCurrency (
        Object user, final int cost, final ResultListener listener)
    {
        // make sure they have the necessary currency to begin with
        final PlayerObject player = (PlayerObject)user;
        if (player.scrip < cost) {
            listener.requestFailed(new InvocationException(
                                       BangCodes.INSUFFICIENT_FUNDS));
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

    protected ArrayList<OfferPublisher> _publishers =
        new ArrayList<OfferPublisher>();
}
