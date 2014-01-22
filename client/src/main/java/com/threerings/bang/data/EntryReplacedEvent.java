//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.SetListener;

import static com.threerings.bang.Log.*;

/**
 * Used to replace a {@link DSet} entry with another entry (with a different key) in such a way
 * that classes "in the know" can distinguish between an entry being removed permanently and
 * an entry being removed temporarily immediately before being replaced.
 */
public class EntryReplacedEvent<T extends DSet.Entry> extends EntryRemovedEvent<T>
{
    /**
     * A dummy added event used to add the new entry.
     */
    public class ReplacementAddedEvent extends EntryAddedEvent<T>
    {
        protected ReplacementAddedEvent ()
        {
            super(EntryReplacedEvent.this._toid, EntryReplacedEvent.this._name, _newEntry);
        }
    }

    /**
     * Constructs a new entry replaced event on the specified target object
     * with the supplied set attribute name, old key, and new entry, and effects the
     * replacement immediately on the server.
     */
    public EntryReplacedEvent (DObject target, String name, Comparable<?> oldKey, T newEntry)
    {
        super(target.getOid(), name, oldKey);
        _newEntry = newEntry;

        try {
            applyToObject(target);
        } catch (ObjectAccessException e) {
            log.warning("Error applying replacement", "target", target.which(), "name", name,
                        "oldKey", oldKey, "newEntry", newEntry, "error", e);
        }
    }

    @Override // documentation inherited
    public boolean applyToObject (DObject target)
        throws ObjectAccessException
    {
        if (_addedEvent != null) {
            return true; // already applied
        } else if (!super.applyToObject(target)) {
            return false;
        }
        return (_addedEvent = new ReplacementAddedEvent()).applyToObject(target);
    }

    @Override // documentation inherited
    protected void notifyListener (Object listener)
    {
        super.notifyListener(listener);
        if (listener instanceof SetListener) {
            @SuppressWarnings("unchecked") SetListener<T> setlist = (SetListener<T>)listener;
            setlist.entryAdded(_addedEvent);
        }
    }

    protected T _newEntry;
    protected transient ReplacementAddedEvent _addedEvent;
}
