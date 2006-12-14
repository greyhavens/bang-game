//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;

/**
 * Contains distributed data for the Hideout.
 */
public class HideoutObject extends PlaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>gangs</code> field. */
    public static final String GANGS = "gangs";

    /** The field name of the <code>topRanked</code> field. */
    public static final String TOP_RANKED = "topRanked";
    // AUTO-GENERATED: FIELDS END

    /** The means by which the client makes requests to the server. */
    public HideoutMarshaller service;

    /** Information concerning all active gangs, for the directory. */
    public DSet<GangEntry> gangs = new DSet<GangEntry>();
    
    /** List of top-ranked gangs for various criteria. */
    public DSet<TopRankedGangList> topRanked = new DSet<TopRankedGangList>();
    
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
    public void removeFromGangs (Comparable key)
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
    public void setGangs (DSet<com.threerings.bang.gang.data.GangEntry> value)
    {
        requestAttributeChange(GANGS, value, this.gangs);
        @SuppressWarnings("unchecked") DSet<com.threerings.bang.gang.data.GangEntry> clone =
            (value == null) ? null : value.typedClone();
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
    public void removeFromTopRanked (Comparable key)
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
    public void setTopRanked (DSet<com.threerings.bang.gang.data.TopRankedGangList> value)
    {
        requestAttributeChange(TOP_RANKED, value, this.topRanked);
        @SuppressWarnings("unchecked") DSet<com.threerings.bang.gang.data.TopRankedGangList> clone =
            (value == null) ? null : value.typedClone();
        this.topRanked = clone;
    }
    // AUTO-GENERATED: METHODS END
}
