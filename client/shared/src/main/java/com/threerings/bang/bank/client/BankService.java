//
// $Id$

package com.threerings.bang.bank.client;

import com.threerings.bang.data.PlayerObject;
import com.threerings.presents.client.InvocationService;

import com.threerings.coin.data.CoinExOfferInfo;

/**
 * Defines the services available to the client at the Bank.
 */
public interface BankService extends InvocationService<PlayerObject>
{
    /** Used by {@link #getMyOffers}. */
    public interface OfferListener extends InvocationListener
    {
        /** Informs the caller of their registered offers. The arrays may be
         * zero length but will not be null. */
        public void gotOffers (CoinExOfferInfo[] buys, CoinExOfferInfo[] sells);
    }

    /**
     * Requests this player's outstanding offers on the exchange.
     */
    public void getMyOffers (OfferListener listener);

    /**
     * Requests that the specified offer be posted to the market.
     *
     * @param buying true if this is a buy offer, false if it is a sell offer.
     * @param immediate true if the offer should be executed immediately or
     * dropped, false if it can be posted to the market.
     * @param rl a result listener that will be notified with a {@link
     * CoinExOfferInfo} for a posted offer or null for an immediately executed
     * transaction.
     */
    public void postOffer (int coins, int pricePerCoin, boolean buying, boolean immediate,
                           ResultListener rl);

    /**
     * Requests that the specified offer be cancelled. This client must, of
     * course, be the originator of the offer.
     */
    public void cancelOffer (int offerId, ConfirmListener cl);
}
