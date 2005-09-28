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
     *
     * @param coins the number of coins to purchase.
     * @param cost the cost computed and displayed on the client; the
     * transaction will only go through if the server can satisfy the request
     * at or below the specified cost.
     */
    public void buyCoins (
        Client client, int coins, int cost, ConfirmListener cl);

    /**
     * Requests that the specified quantity of coins be sold immediately to
     * offers available on the market.
     *
     * @param coins the number of coins to sell.
     * @param earnings the earnings computed and displayed on the client; the
     * transaction will only go through if the server can satisfy the request
     * at or above the specified earnings.
     */
    public void sellCoins (
        Client client, int coins, int earnings, ConfirmListener cl);

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
