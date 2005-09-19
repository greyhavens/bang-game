//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;

/**
 * Contains distributed data for the General Store.
 */
public class StoreObject extends PlaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>goods</code> field. */
    public static final String GOODS = "goods";
    // AUTO-GENERATED: FIELDS END

    /** The goods available for sale in this store. */
    public DSet goods;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>goods</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToGoods (DSet.Entry elem)
    {
        requestEntryAdd(GOODS, goods, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>goods</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromGoods (Comparable key)
    {
        requestEntryRemove(GOODS, goods, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>goods</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateGoods (DSet.Entry elem)
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
    public void setGoods (DSet value)
    {
        requestAttributeChange(GOODS, value, this.goods);
        this.goods = (value == null) ? null : (DSet)value.clone();
    }
    // AUTO-GENERATED: METHODS END
}
