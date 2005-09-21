//
// $Id$

package com.threerings.bang.data;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.presents.dobj.DSet;

import com.threerings.bang.client.ItemIcon;

/**
 * The base class for all items in Bang! Howdy.
 */
public abstract class Item
    implements DSet.Entry, Cloneable
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
     * Creates an icon for use in displaying this item. The icon <em>must</em>
     * be configured with the item and a reference to the client context via a
     * subsequent call to {@link ItemIcon#setItem}. If this method returns
     * null, it will be assumed that items of this type should not appear in
     * the standard inventory displays (Big Shot items, for example, are
     * handled specially, as perhaps will avatar items).
     */
    public ItemIcon createIcon ()
    {
        return new ItemIcon();
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

    @Override // documentation inherited
    public int hashCode ()
    {
        return _itemId.intValue();
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException("The sky is falling!");
        }
    }

    @Override // documentation inherited
    public String toString ()
    {
        StringBuffer buf = new StringBuffer("[");
        toString(buf);
        return buf.append("]").toString();
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

    /**
     * Allows derived classes to augment our {@link #toString} output.
     */
    protected void toString (StringBuffer buf)
    {
        buf.append("itemId=").append(_itemId);
        buf.append(", ownerId=").append(_ownerId);
    }

    /** This item's unique identifier. */
    protected transient Integer _itemId = Integer.valueOf(0);

    /** The id of the user that owns this item. */
    protected transient int _ownerId;

    /** Used when serializing this item for storage in the database. */
    protected transient boolean _nondb = true;
}
