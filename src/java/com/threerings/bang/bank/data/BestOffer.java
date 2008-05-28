//
// $Id$

package com.threerings.bang.bank.data;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService.ResultListener;
import com.threerings.presents.dobj.ChangeListener;

import com.threerings.bang.data.ConsolidatedOffer;

/**
 * An interface for getting best buy/sell offers from an exchange object.
 */
public interface BestOffer
{
    /**
     * Returns a consolidation of the best published buy offers (the price will
     * be the best buy price and the volume will be the total volume of all
     * offers at that price) or null if there are no published buy offers.
     */
    public ConsolidatedOffer getBestBuy ();

    /**
     * Returns a consolidation of the best published sell offers (the price
     * will be the best sell price and the volume will be the total volume of
     * all offers at that price) or null if there are no published sell offers.
     */
    public ConsolidatedOffer getBestSell ();

    /**
     * Requests that the specified offer be posted to the market.
     *
     * @param buying true if this is a buy offer, false if it is a sell offer.
     * @param rl a result listener that will be notified with a {@link
     * CoinExOfferInfo} for a posted offer or null for an immediately executed
     * transaction.
     */
    public void postImmediateOffer (
        Client client, int coins, int pricePerCoin, boolean buying, ResultListener rl);

    public void addListener (ChangeListener listener);
}
