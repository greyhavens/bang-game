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
     * Requests that the specified quantity of coins be purchased immediately
     * from offers available on the market.
     */
    public void buyCoins (
        Client client, int coins, int pricePerCoin, ConfirmListener cl);

    /**
     * Requests that the specified quantity of coins be sold immediately to
     * offers available on the market.
     */
    public void sellCoins (
        Client client, int coins, int pricePerCoin, ConfirmListener cl);

    /**
     * Requests that the specified buy offer be posted to the market.
     */
    public void postBuyOffer (
        Client client, int coins, int pricePerCoin, ConfirmListener cl);

    /**
     * Requests that the specified sell offer be posted to the market.
     */
    public void postSellOffer (
        Client client, int coins, int pricePerCoin, ConfirmListener cl);
}
