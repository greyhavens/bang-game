//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.ConsolidatedOffer;
import com.threerings.bang.data.Item;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.GoodsObject;

/**
 * Contains distributed data for the Hideout.
 */
public class HideoutObject extends PlaceObject
    implements GoodsObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>gangs</code> field. */
    public static final String GANGS = "gangs";

    /** The field name of the <code>topRanked</code> field. */
    public static final String TOP_RANKED = "topRanked";

    /** The field name of the <code>goods</code> field. */
    public static final String GOODS = "goods";

    /** The field name of the <code>rentalGoods</code> field. */
    public static final String RENTAL_GOODS = "rentalGoods";
    // AUTO-GENERATED: FIELDS END

    /** The means by which the client makes requests to the server. */
    public HideoutMarshaller service;

    /** Information concerning all active gangs, for the directory. */
    public DSet<GangEntry> gangs = new DSet<GangEntry>();

    /** List of top-ranked gangs for various criteria. */
    public DSet<TopRankedGangList> topRanked = new DSet<TopRankedGangList>();

    /** Gang goods available for sale. */
    public DSet<Good> goods;

    /** The rental goods available for sale. */
    public DSet<Good> rentalGoods;

    // documentation inherited from interface GoodsObject
    public DSet<Good> getGoods ()
    {
        return goods;
    }

    // documentation inherited from interface GoodsObject
    public void buyGood (String type, Object[] args, InvocationService.ConfirmListener cl)
    {
        service.buyGangGood(type, args, cl);
    }

    /**
     * Returns the rental good that can be used to create the supplied item.
     */
    public RentalGood getRentalGood (Item item)
    {
        for (Good good : rentalGoods) {
            if (good.wouldCreateItem(item)) {
                return (RentalGood)good;
            }
        }
        return null;
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
    public void setService (HideoutMarshaller value)
    {
        HideoutMarshaller ovalue = this.service;
        requestAttributeChange(
            SERVICE, value, ovalue);
        this.service = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>gangs</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToGangs (GangEntry elem)
    {
        requestEntryAdd(GANGS, gangs, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>gangs</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromGangs (Comparable<?> key)
    {
        requestEntryRemove(GANGS, gangs, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>gangs</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateGangs (GangEntry elem)
    {
        requestEntryUpdate(GANGS, gangs, elem);
    }

    /**
     * Requests that the <code>gangs</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setGangs (DSet<GangEntry> value)
    {
        requestAttributeChange(GANGS, value, this.gangs);
        DSet<GangEntry> clone = (value == null) ? null : value.clone();
        this.gangs = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>topRanked</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToTopRanked (TopRankedGangList elem)
    {
        requestEntryAdd(TOP_RANKED, topRanked, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>topRanked</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromTopRanked (Comparable<?> key)
    {
        requestEntryRemove(TOP_RANKED, topRanked, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>topRanked</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateTopRanked (TopRankedGangList elem)
    {
        requestEntryUpdate(TOP_RANKED, topRanked, elem);
    }

    /**
     * Requests that the <code>topRanked</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setTopRanked (DSet<TopRankedGangList> value)
    {
        requestAttributeChange(TOP_RANKED, value, this.topRanked);
        DSet<TopRankedGangList> clone = (value == null) ? null : value.clone();
        this.topRanked = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>goods</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToGoods (Good elem)
    {
        requestEntryAdd(GOODS, goods, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>goods</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromGoods (Comparable<?> key)
    {
        requestEntryRemove(GOODS, goods, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>goods</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateGoods (Good elem)
    {
        requestEntryUpdate(GOODS, goods, elem);
    }

    /**
     * Requests that the <code>goods</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setGoods (DSet<Good> value)
    {
        requestAttributeChange(GOODS, value, this.goods);
        DSet<Good> clone = (value == null) ? null : value.clone();
        this.goods = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>rentalGoods</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToRentalGoods (Good elem)
    {
        requestEntryAdd(RENTAL_GOODS, rentalGoods, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>rentalGoods</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromRentalGoods (Comparable<?> key)
    {
        requestEntryRemove(RENTAL_GOODS, rentalGoods, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>rentalGoods</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateRentalGoods (Good elem)
    {
        requestEntryUpdate(RENTAL_GOODS, rentalGoods, elem);
    }

    /**
     * Requests that the <code>rentalGoods</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setRentalGoods (DSet<Good> value)
    {
        requestAttributeChange(RENTAL_GOODS, value, this.rentalGoods);
        DSet<Good> clone = (value == null) ? null : value.clone();
        this.rentalGoods = clone;
    }
    // AUTO-GENERATED: METHODS END
}
