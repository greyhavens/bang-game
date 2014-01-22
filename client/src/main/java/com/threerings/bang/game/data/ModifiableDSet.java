//
// $Id$

package com.threerings.bang.game.data;

import java.util.Iterator;

import com.threerings.presents.dobj.DSet;

/**
 * A {@link DSet} that permits local modifications in order to allow effects to enact changes to
 * distributed objects on both client and server.
 */
public class ModifiableDSet<T extends DSet.Entry> extends DSet<T>
{
    public ModifiableDSet (Iterator<T> entries)
    {
        super(entries);
    }

    public ModifiableDSet (T[] entries)
    {
        super(entries);
    }

    public ModifiableDSet ()
    {
    }

    /**
     * Adds an entry directly to this distributed set without creating the
     * necessary distributed events to make things work properly. This
     * exists so that effects (which are "applied") on the client and
     * server can add entries to certain distributed sets.
     */
    public void addDirect (T entry)
    {
        add(entry);
    }

    /**
     * Removes an entry directly from this distributed set without creating
     * the necessary distributed events to make things work properly. This
     * exists so that effects (which are "applied") on the client and
     * server can remove entries from certain distributed sets.
     */
    public boolean removeDirect (T entry)
    {
        return remove(entry);
    }

    /**
     * Updates an entry directly in this distributed set without creating the
     * necessary distributed events to make things work properly.  This exists
     * so that effects (which are "applied") on the client and server can
     * update certain distributed sets.
     */
    public T updateDirect (T entry)
    {
        return update(entry);
    }

    @Override
    public ModifiableDSet<T> clone ()
    {
        return (ModifiableDSet<T>)super.clone();
    }
}
