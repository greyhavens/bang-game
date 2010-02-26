//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;

/**
 * Contains distributed data for the Saloon.
 */
public class SaloonObject extends PlaceObject
    implements TopRankObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>parlors</code> field. */
    public static final String PARLORS = "parlors";

    /** The field name of the <code>topRanked</code> field. */
    public static final String TOP_RANKED = "topRanked";
    // AUTO-GENERATED: FIELDS END

    /** The means by which the client makes requests to the server. */
    public SaloonMarshaller service;

    /** Contains info on all active back parlors. */
    public DSet<ParlorInfo> parlors = new DSet<ParlorInfo>();

    /** Contains info on the top-ranked players by various criterion. */
    public DSet<TopRankedList> topRanked = new DSet<TopRankedList>();

    // documentation inherited from interface TopRankObject
    public DSet<TopRankedList> getTopRanked ()
    {
        return topRanked;
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
    public void setService (SaloonMarshaller value)
    {
        SaloonMarshaller ovalue = this.service;
        requestAttributeChange(
            SERVICE, value, ovalue);
        this.service = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>parlors</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToParlors (ParlorInfo elem)
    {
        requestEntryAdd(PARLORS, parlors, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>parlors</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromParlors (Comparable<?> key)
    {
        requestEntryRemove(PARLORS, parlors, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>parlors</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateParlors (ParlorInfo elem)
    {
        requestEntryUpdate(PARLORS, parlors, elem);
    }

    /**
     * Requests that the <code>parlors</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setParlors (DSet<ParlorInfo> value)
    {
        requestAttributeChange(PARLORS, value, this.parlors);
        DSet<ParlorInfo> clone = (value == null) ? null : value.clone();
        this.parlors = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>topRanked</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToTopRanked (TopRankedList elem)
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
    public void updateTopRanked (TopRankedList elem)
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
    public void setTopRanked (DSet<TopRankedList> value)
    {
        requestAttributeChange(TOP_RANKED, value, this.topRanked);
        DSet<TopRankedList> clone = (value == null) ? null : value.clone();
        this.topRanked = clone;
    }
    // AUTO-GENERATED: METHODS END
}
