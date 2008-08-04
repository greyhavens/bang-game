//
// $Id$

package com.threerings.bang.gang.server.persist;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;

import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.OutfitArticle;

/**
 * Contains information loaded from the database about a gang
 */
public class GangRecord
{
    /** The gang's unique identifier. */
    public int gangId;

    /** The name of the gang. */
    public String name;

    /** The normalized name of the gang (used to avoid name collisions). */
    public String normalized;

    /** The date upon which the gang was founded. */
    public Timestamp founded;

    /** The gang's statement. */
    public String statement;

    /** The gang's home page. */
    public String url;

    /** The gang's weight class (determined by the upgrades it holds, but stored for queries). */
    public byte weightClass;

    /** The gang's accumulated notoriety points. */
    public int notoriety;

    /** The last time notoriety points were added. */
    public Timestamp lastPlayed;

    /** The amount of scrip in the gang's coffers. */
    public int scrip;

    /** The number of aces in the gang's coffers. */
    public int aces;

    /** The encoded buckle (item ids of the parts used). */
    public byte[] buckle;

    /** The cached buckle fingerprint. */
    public byte[] bucklePrint;

    /** The number of coins in the gang's coffers. */
    public transient int coins;

    /** The items owned by the gang. */
    public transient List<Item> inventory;

    /** The currently configured gang outfit. */
    public transient OutfitArticle[] outfit;

    /** The members of the gang. */
    public transient List<GangMemberEntry> members;

    /** The avatar of the most senior leader. */
    public transient AvatarInfo avatar;

    /** Used when creating new gangs. */
    public GangRecord (Handle name)
    {
        this.name = name.toString();
        normalized = name.getNormal();
        statement = "";
        url = "";
        buckle = new byte[0];
        bucklePrint = new byte[0];
        inventory = new ArrayList<Item>();
        outfit = new OutfitArticle[0];
        members = new ArrayList<GangMemberEntry>();
    }

    /** Used when forming queries. */
    public GangRecord (int gangId)
    {
        this.gangId = gangId;
    }

    /** Used when loading records from the database. */
    public GangRecord ()
    {
    }

    /** Returns the gang name as a {@link Handle}. */
    public Handle getName ()
    {
        return new Handle(name);
    }

    /** Returns the name used to identity the gang's entry in the coin database. */
    public String getCoinAccount ()
    {
        return "{" + name + "}";
    }

    /**
     * Returns the buckle as an array of integers representing the item ids of the
     * parts used.
     */
    public int[] getBuckle ()
    {
        return decode(buckle);
    }

    /**
     * Returns the buckle fingerprint.
     */
    public int[] getBucklePrint ()
    {
        return decode(bucklePrint);
    }

    /**
     * Sets the buckle field.
     */
    public void setBuckle (int[] buckle, int[] print)
    {
        this.buckle = encode(buckle);
        bucklePrint = encode(print);
    }

    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return "[gangId=" + gangId + ", name=" + name + ", founded=" +
            founded + ", scrip=" + scrip + "]";
    }

    protected static byte[] encode (int[] values)
    {
        byte[] data = new byte[values.length * 4];
        ByteBuffer.wrap(data).asIntBuffer().put(values);
        return data;
    }

    protected static int[] decode (byte[] data)
    {
        int[] values = new int[data.length / 4];
        ByteBuffer.wrap(data).asIntBuffer().get(values);
        return values;
    }
}
