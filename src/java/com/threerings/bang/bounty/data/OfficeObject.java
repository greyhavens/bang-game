//
// $Id$

package com.threerings.bang.bounty.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;

/**
 * Defines the data model for the Sheriff's Office location.
 */
public class OfficeObject extends PlaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>boards</code> field. */
    public static final String BOARDS = "boards";

    /** The field name of the <code>completers</code> field. */
    public static final String COMPLETERS = "completers";

    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";
    // AUTO-GENERATED: FIELDS END

    /** Contains metadata on all available boards. */
    public DSet<BoardInfo> boards;

    /** Contains recent completers for all bounties. */
    public DSet<RecentCompleters> completers;

    /** Provides office-related services. */
    public OfficeMarshaller service;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>boards</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToBoards (BoardInfo elem)
    {
        requestEntryAdd(BOARDS, boards, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>boards</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromBoards (Comparable<?> key)
    {
        requestEntryRemove(BOARDS, boards, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>boards</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateBoards (BoardInfo elem)
    {
        requestEntryUpdate(BOARDS, boards, elem);
    }

    /**
     * Requests that the <code>boards</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setBoards (DSet<BoardInfo> value)
    {
        requestAttributeChange(BOARDS, value, this.boards);
        DSet<BoardInfo> clone = (value == null) ? null : value.clone();
        this.boards = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>completers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToCompleters (RecentCompleters elem)
    {
        requestEntryAdd(COMPLETERS, completers, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>completers</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromCompleters (Comparable<?> key)
    {
        requestEntryRemove(COMPLETERS, completers, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>completers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateCompleters (RecentCompleters elem)
    {
        requestEntryUpdate(COMPLETERS, completers, elem);
    }

    /**
     * Requests that the <code>completers</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setCompleters (DSet<RecentCompleters> value)
    {
        requestAttributeChange(COMPLETERS, value, this.completers);
        DSet<RecentCompleters> clone = (value == null) ? null : value.clone();
        this.completers = clone;
    }

    /**
     * Requests that the <code>service</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setService (OfficeMarshaller value)
    {
        OfficeMarshaller ovalue = this.service;
        requestAttributeChange(
            SERVICE, value, ovalue);
        this.service = value;
    }
    // AUTO-GENERATED: METHODS END
}
