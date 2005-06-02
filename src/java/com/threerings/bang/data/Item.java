//
// $Id$

package com.threerings.bang.data;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.presents.dobj.DSet;

/**
 * The base class for all items in Bang! Howdy.
 */
public abstract class Item
    implements DSet.Entry
{
    /**
     * Returns the key used to identify this item in a {@link DSet}.
     */
    public Comparable getKey ()
    {
        return _itemId;
    }

    /**
     * Returns this item's unique identifier.
     */
    public int getItemId ()
    {
        return _itemId.intValue();
    }

    /**
     * Returns the user id of the owner of this item.
     */
    public int getOwnerId ()
    {
        return _ownerId;
    }

    /**
     * Called by the item repository when an item is loaded from the
     * database.
     */
    public void setItemId (int itemId)
    {
        _itemId = itemId;
    }

    /**
     * Called by the item repository when an item is loaded from the
     * database.
     */
    public void setOwnerId (int ownerId)
    {
        _ownerId = ownerId;
    }

    /**
     * Writes our custom streamable fields.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        if (_nondb) {
            out.writeInt(getItemId());
            out.writeInt(getOwnerId());
        }
        out.defaultWriteObject();
    }

    /**
     * Reads our custom streamable fields.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        if (_nondb) {
            _itemId = new Integer(in.readInt());
            _ownerId = in.readInt();
        }
        in.defaultReadObject();
    }

    /**
     * Serializes this instance for storage in the item database.
     */
    public void persistTo (ObjectOutputStream out)
        throws IOException
    {
        _nondb = false;
        try {
            out.writeBareObject(this);
        } finally {
            _nondb = true;
        }
    }

    /**
     * Unserializes this item from data obtained from the item database.
     */
    public void unpersistFrom (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        _nondb = false;
        try {
            in.readBareObject(this);
        } finally {
            _nondb = true;
        }
    }

    /**
     * Returns true if the specified object is an item and has the same
     * item id as this item.
     */
    public boolean equals (Object other)
    {
        if (other instanceof Item) {
            return ((Item)other)._itemId.equals(_itemId);
        } else {
            return false;
        }
    }

    // documentation inherited (Object)
    public int hashCode ()
    {
        return _itemId.intValue();
    }

    /**
     * Used by derived classes when an item is newly constructed.
     */
    protected Item (int ownerId)
    {
        _ownerId = ownerId;
    }

    /** Creates a blank item, used only during unserialization. */
    protected Item ()
    {
    }

    /** This item's unique identifier. */
    protected transient Integer _itemId = Integer.valueOf(0);

    /** The id of the user that owns this item. */
    protected transient int _ownerId;

    /** Used when serializing this item for storage in the database. */
    protected transient boolean _nondb = true;
}
