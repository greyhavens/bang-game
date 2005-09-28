//
// $Id$

package com.threerings.bang.bank.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;

/**
 * Contains published information for the Bank.
 */
public class BankObject extends PlaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>buyOffers</code> field. */
    public static final String BUY_OFFERS = "buyOffers";

    /** The field name of the <code>sellOffers</code> field. */
    public static final String SELL_OFFERS = "sellOffers";
    // AUTO-GENERATED: FIELDS END

    /** Provides access to the bank invocation service. */
    public BankMarshaller service;

    /** The top N offers to buy coins for scrip. */
    public DSet buyOffers;

    /** The top N offers to sell coins for scrip. */
    public DSet sellOffers;

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
     * Requests that the specified entry be added to the
     * <code>buyOffers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToBuyOffers (DSet.Entry elem)
    {
        requestEntryAdd(BUY_OFFERS, buyOffers, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>buyOffers</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromBuyOffers (Comparable key)
    {
        requestEntryRemove(BUY_OFFERS, buyOffers, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>buyOffers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateBuyOffers (DSet.Entry elem)
    {
        requestEntryUpdate(BUY_OFFERS, buyOffers, elem);
    }

    /**
     * Requests that the <code>buyOffers</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setBuyOffers (DSet value)
    {
        requestAttributeChange(BUY_OFFERS, value, this.buyOffers);
        this.buyOffers = (value == null) ? null : (DSet)value.clone();
    }

    /**
     * Requests that the specified entry be added to the
     * <code>sellOffers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToSellOffers (DSet.Entry elem)
    {
        requestEntryAdd(SELL_OFFERS, sellOffers, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>sellOffers</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromSellOffers (Comparable key)
    {
        requestEntryRemove(SELL_OFFERS, sellOffers, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>sellOffers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateSellOffers (DSet.Entry elem)
    {
        requestEntryUpdate(SELL_OFFERS, sellOffers, elem);
    }

    /**
     * Requests that the <code>sellOffers</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setSellOffers (DSet value)
    {
        requestAttributeChange(SELL_OFFERS, value, this.sellOffers);
        this.sellOffers = (value == null) ? null : (DSet)value.clone();
    }
    // AUTO-GENERATED: METHODS END
}
