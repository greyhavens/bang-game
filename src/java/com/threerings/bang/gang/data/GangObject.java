//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.util.Name;

/**
 * Contains data concerning a single gang.
 */
public class GangObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>gangId</code> field. */
    public static final String GANG_ID = "gangId";

    /** The field name of the <code>name</code> field. */
    public static final String NAME = "name";

    /** The field name of the <code>founded</code> field. */
    public static final String FOUNDED = "founded";

    /** The field name of the <code>members</code> field. */
    public static final String MEMBERS = "members";
    // AUTO-GENERATED: FIELDS END

    /** This gang's unique identifier. */
    public int gangId;

    /** The name of this gang. */
    public Name name;
    
    /** The day on which this gang was founded. */
    public long founded;
    
    /** Contains a {@link GangMemberInfo} for each member of this gang. */
    public DSet<GangMemberEntry> members = new DSet<GangMemberEntry>();

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>gangId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGangId (int value)
    {
        int ovalue = this.gangId;
        requestAttributeChange(
            GANG_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.gangId = value;
    }

    /**
     * Requests that the <code>name</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setName (Name value)
    {
        Name ovalue = this.name;
        requestAttributeChange(
            NAME, value, ovalue);
        this.name = value;
    }

    /**
     * Requests that the <code>founded</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setFounded (long value)
    {
        long ovalue = this.founded;
        requestAttributeChange(
            FOUNDED, Long.valueOf(value), Long.valueOf(ovalue));
        this.founded = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>members</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToMembers (GangMemberEntry elem)
    {
        requestEntryAdd(MEMBERS, members, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>members</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMembers (Comparable key)
    {
        requestEntryRemove(MEMBERS, members, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>members</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMembers (GangMemberEntry elem)
    {
        requestEntryUpdate(MEMBERS, members, elem);
    }

    /**
     * Requests that the <code>members</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setMembers (DSet<com.threerings.bang.gang.data.GangMemberEntry> value)
    {
        requestAttributeChange(MEMBERS, value, this.members);
        @SuppressWarnings("unchecked") DSet<com.threerings.bang.gang.data.GangMemberEntry> clone =
            (value == null) ? null : value.typedClone();
        this.members = clone;
    }
    // AUTO-GENERATED: METHODS END
}
