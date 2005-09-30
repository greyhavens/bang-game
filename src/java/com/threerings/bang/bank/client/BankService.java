//
// $Id$

package com.threerings.bang.bank.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Defines the services available to the client at the Bank.
 */
public interface BankService extends InvocationService
{
    /**
     * Requests that the specified offer be posted to the market.
     *
     * @param buying true if this is a buy offer, false if it is a sell offer.
     * @param immediate true if the offer should be executed immediately or
     * dropped, false if it can be posted to the market.
     */
    public void postOffer (
        Client client, int coins, int pricePerCoin, boolean buying,
        boolean immediate, ConfirmListener cl);
}
