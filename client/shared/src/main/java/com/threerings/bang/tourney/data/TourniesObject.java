//
// $Id$

package com.threerings.bang.tourney.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

/**
 * Contains a list of all active tournies.
 */
public class TourniesObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>tournies</code> field. */
    public static final String TOURNIES = "tournies";
    // AUTO-GENERATED: FIELDS END

    /** All the active tournies. */
    public DSet<TourneyListingEntry> tournies = new DSet<TourneyListingEntry>();

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>tournies</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToTournies (TourneyListingEntry elem)
    {
        requestEntryAdd(TOURNIES, tournies, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>tournies</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromTournies (Comparable<?> key)
    {
        requestEntryRemove(TOURNIES, tournies, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>tournies</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateTournies (TourneyListingEntry elem)
    {
        requestEntryUpdate(TOURNIES, tournies, elem);
    }

    /**
     * Requests that the <code>tournies</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setTournies (DSet<TourneyListingEntry> value)
    {
        requestAttributeChange(TOURNIES, value, this.tournies);
        DSet<TourneyListingEntry> clone = (value == null) ? null : value.clone();
        this.tournies = clone;
    }
    // AUTO-GENERATED: METHODS END

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.presents.dobj.DObject.class, "_oid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.tourney.data.TourniesObject.class, "tournies", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.presents.dobj.DObject.class, "_oid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.tourney.data.TourniesObject.class, "tournies", this, out);
    }
}
