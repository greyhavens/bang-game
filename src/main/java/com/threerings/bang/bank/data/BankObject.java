//
// $Id$

package com.threerings.bang.bank.data;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService.ResultListener;

import com.threerings.bang.data.ConsolidatedOffer;

/**
 * Contains published information for the Bank.
 */
public class BankObject extends PlaceObject
    implements BestOffer
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>lastTrade</code> field. */
    public static final String LAST_TRADE = "lastTrade";

    /** The field name of the <code>buyOffers</code> field. */
    public static final String BUY_OFFERS = "buyOffers";

    /** The field name of the <code>sellOffers</code> field. */
    public static final String SELL_OFFERS = "sellOffers";
    // AUTO-GENERATED: FIELDS END

    /** Provides access to the bank invocation service. */
    public BankMarshaller service;

    /** The price of the last transaction on the exchange. */
    public int lastTrade;

    /** The top N offers to buy coins for scrip. */
    public ConsolidatedOffer[] buyOffers;

    /** The top N offers to sell coins for scrip. */
    public ConsolidatedOffer[] sellOffers;

    // documentation inherited from BestOffer
    public ConsolidatedOffer getBestBuy ()
    {
        ConsolidatedOffer best = null;
        for (int ii = 0; ii < buyOffers.length; ii++) {
            if (best == null || buyOffers[ii].price > best.price) {
                best = buyOffers[ii];
            }
        }
        return best;
    }

    // documentation inherited from BestOffer
    public ConsolidatedOffer getBestSell ()
    {
        ConsolidatedOffer best = null;
        for (int ii = 0; ii < sellOffers.length; ii++) {
            if (best == null || sellOffers[ii].price < best.price) {
                best = sellOffers[ii];
            }
        }
        return best;
    }

    // documentation inherited from BestOffer
    public void postImmediateOffer (
        Client client, int coins, int pricePerCoin, boolean buying, ResultListener rl)
    {
        service.postOffer(client, coins, pricePerCoin, buying, true, rl);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>service</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setService (BankMarshaller value)
    {
        BankMarshaller ovalue = this.service;
        requestAttributeChange(
            SERVICE, value, ovalue);
        this.service = value;
    }

    /**
     * Requests that the <code>lastTrade</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setLastTrade (int value)
    {
        int ovalue = this.lastTrade;
        requestAttributeChange(
            LAST_TRADE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.lastTrade = value;
    }

    /**
     * Requests that the <code>buyOffers</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBuyOffers (ConsolidatedOffer[] value)
    {
        ConsolidatedOffer[] ovalue = this.buyOffers;
        requestAttributeChange(
            BUY_OFFERS, value, ovalue);
        this.buyOffers = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>buyOffers</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setBuyOffersAt (ConsolidatedOffer value, int index)
    {
        ConsolidatedOffer ovalue = this.buyOffers[index];
        requestElementUpdate(
            BUY_OFFERS, index, value, ovalue);
        this.buyOffers[index] = value;
    }

    /**
     * Requests that the <code>sellOffers</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setSellOffers (ConsolidatedOffer[] value)
    {
        ConsolidatedOffer[] ovalue = this.sellOffers;
        requestAttributeChange(
            SELL_OFFERS, value, ovalue);
        this.sellOffers = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>sellOffers</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setSellOffersAt (ConsolidatedOffer value, int index)
    {
        ConsolidatedOffer ovalue = this.sellOffers[index];
        requestElementUpdate(
            SELL_OFFERS, index, value, ovalue);
        this.sellOffers[index] = value;
    }
    // AUTO-GENERATED: METHODS END
}
