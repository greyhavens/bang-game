//
// $Id$

package com.threerings.bang.data;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.jme.renderer.Renderer;
import com.jmex.bui.icon.ImageIcon;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.presents.dobj.DSet;
import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BasicContext;

/**
 * The base class for all items in Bang! Howdy.
 */
public abstract class Item
    implements DSet.Entry, Cloneable
{
    /** Date formating when showing temporary articles. */
    public static final SimpleDateFormat EXPIRE_FORMAT = new SimpleDateFormat("MMM d, yyyy");

    /**
     * Returns the key used to identify this item in a {@link DSet}.
     */
    public Comparable<?> getKey ()
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
     * Checks whether or not this item is owned by a gang, as opposed to a user.
     */
    public boolean isGangOwned ()
    {
        return _gangOwned;
    }

    /**
     * Returns the user or gang id of the owner of this item.
     */
    public int getOwnerId ()
    {
        return _ownerId;
    }

    /**
     * Called by the item repository when an item is loaded from the database.
     */
    public void setItemId (int itemId)
    {
        _itemId = itemId;
    }

    /**
     * Called by the item repository when an item is loaded from the database.
     */
    public void setGangOwned (boolean gangOwned)
    {
        _gangOwned = gangOwned;
    }

    /**
     * Called by the item repository when an item is loaded from the database.
     */
    public void setOwnerId (int ownerId)
    {
        _ownerId = ownerId;
    }

    /**
     * Returns a qualified translatable string describing this item.
     */
    public abstract String getName ();

    /**
     * Returns a qualified translatable string describing this item.
     */
    public String getName (boolean small)
    {
        return getName();
    }

    /**
     * Returns the gang id for a gang rented item, or 0 if it is owned by the user.
     */
    public int getGangId ()
    {
        return _gangId;
    }

    /**
     * Called by the item repository when an item is loaded from the database.
     */
    public void setGangId (int gangId)
    {
        _gangId = gangId;
    }

    /**
     * Returns the expire time as a java.sql.Date.
     */
    public java.sql.Date getExpiryDate ()
    {
        if (_rentExpires == 0) {
            return null;
        }
        return new java.sql.Date(_rentExpires);
    }

    /**
     * Returns the expired time in milliseconds.
     */
    public long getExpires ()
    {
        return _rentExpires;
    }

    /**
     * Called by the item repository when an item is loaded from the database.
     */
    public void setExpires (Date expires)
    {
        setExpires(expires == null ? 0 : expires.getTime());
    }

    /**
     * Called by the item repository when an item is loaded from the database.
     */
    public void setExpires (long expires)
    {
        _rentExpires = expires;
    }

    /**
     * Determines whether this item has expired.
     */
    public boolean isExpired (long timestamp)
    {
        if (_rentExpires == 0) {
            return false;
        }
        Date expire = new Date(_rentExpires);
        return expire.before(new Date(timestamp));
    }

    public String getTooltipText (PlayerObject user)
    {
        String msg = getTooltip(user);
        if (_gangId != 0) {
            msg = MessageBundle.compose("m.item_gang", msg);
        }
        if (_rentExpires != 0) {
            msg = MessageBundle.compose("m.item_expires", msg,
                    MessageBundle.taint(EXPIRE_FORMAT.format(new Date(_rentExpires))));
        }
        return msg;
    }

    /**
     * Returns a qualified translatable string to display in a tooltip when the player is hovering
     * over this item's icon.
     */
    public abstract String getTooltip (PlayerObject user);

    /**
     * Returns the path to the icon to use for this item.
     */
    public abstract String getIconPath ();

    /**
     * Returns the path to the icon to use for this item.
     *
     * @param small if true return a small version of the icon.
     */
    public String getIconPath (boolean small)
    {
        return getIconPath();
    }

    /**
     * Generates an icon for this item.
     */
    public ImageIcon createIcon (BasicContext ctx, String iconPath)
    {
        final ImageIcon icon = buildIcon(ctx, iconPath);
        String image = null;
        if (!isGangOwned() && _gangId != 0) {
            image = "ui/icons/gang_item.png";
        }
        if (_rentExpires != 0) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
            Date expires = new Date(_rentExpires);
            for (int ii = 0; ii < 7; ii++) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
                if (expires.before(cal.getTime())) {
                    image = "ui/icons/expires" + (ii < 3 ? "_" + ii + ".png" : "_soon.png");
                    break;
                }
            }
        }
        if (image == null) {
            return icon;
        }
        return new ImageIcon(ctx.loadImage(image)) {
            public int getHeight () {
                return icon.getHeight();
            }
            public int getWidth () {
                return icon.getWidth();
            }
            public void wasAdded () {
                super.wasAdded();
                icon.wasAdded();
            }
            public void wasRemoved () {
                super.wasRemoved();
                icon.wasRemoved();
            }
            public void render (Renderer r, int x, int y, float alpha) {
                icon.render(r, x, y, alpha);
                super.render(r, x, y, alpha);
            }
        };
    }

    /**
     * If an item needs to colorize its icon image or otherwise do something special, it can
     * override this method.
     */
    protected ImageIcon buildIcon (BasicContext ctx, String iconPath)
    {
        return new ImageIcon(ctx.loadImage(iconPath));
    }

    /**
     * Writes our custom streamable fields.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        if (_nondb) {
            out.writeInt(getItemId());
            out.writeBoolean(isGangOwned());
            out.writeInt(getOwnerId());
            out.writeInt(getGangId());
            out.writeLong(_rentExpires);
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
            _gangOwned = in.readBoolean();
            _ownerId = in.readInt();
            _gangId = in.readInt();
            _rentExpires = in.readLong();
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
     * Returns true if the specified object is an item and has the same item id as this item.
     */
    public boolean equals (Object other)
    {
        if (other instanceof Item) {
            return ((Item)other)._itemId.equals(_itemId);
        } else {
            return false;
        }
    }

    /**
     * Compares this item to another in terms of its content, rather than its identity
     * (for example, articles of the same type with the same colorizations are equal
     * in content).
     */
    public boolean isEquivalent (Item other)
    {
        // must be of the exact same class
        return (other != null && getClass() == other.getClass());
    }

    /**
     * Determines whether this item can be destroyed.
     */
    public boolean isDestroyable (PlayerObject user)
    {
        // TODO: Determine which items (besides articles used in looks) should be
        // indestructable
        return true;
    }

    /**
     * Determines whether users can have exact duplicates of this item.
     */
    public boolean allowsDuplicates ()
    {
        return false;
    }

    /**
     * Determines if a user can own this item.
     */
    public boolean canBeOwned (PlayerObject user)
    {
        return true;
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
            throw new RuntimeException(cnse);
        }
    }

    @Override // documentation inherited
    public String toString ()
    {
        StringBuilder buf = new StringBuilder("[");
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
    protected void toString (StringBuilder buf)
    {
        buf.append("itemId=").append(_itemId);
        buf.append(_gangOwned ? ", gangId=" : ", ownerId=").append(_ownerId);
    }

    /** This item's unique identifier. */
    protected transient Integer _itemId = Integer.valueOf(0);

    /** If true, the item is owned by a gang rather than a player. */
    protected transient boolean _gangOwned;

    /** The id of the user or gang that owns this item. */
    protected transient int _ownerId;

    /** The id of the gang this item is from if owned by a user. */
    protected transient int _gangId;

    /** The time when this item will expire. */
    protected transient long _rentExpires;

    /** Used when serializing this item for storage in the database. */
    protected transient boolean _nondb = true;
}
