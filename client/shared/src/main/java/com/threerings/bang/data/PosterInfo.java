//
// $Id$

package com.threerings.bang.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.StreamableArrayList;
import com.threerings.util.StreamableHashMap;
import com.threerings.presents.dobj.DSet;

/**
 * Contains data for a player's wanted poster.
 */
public class PosterInfo extends SimpleStreamableObject
    implements DSet.Entry, Cloneable
{
    public static final int BADGES = 4;

    public static class RankGroup extends SimpleStreamableObject
    {
        public long week;
        public StreamableHashMap<String,Integer> rankings;

        public RankGroup () { }

        public RankGroup (long week, StreamableHashMap<String, Integer> rankings)
        {
            this.week = week;
            this.rankings = rankings;
        }

        // from interface Streamable
        public void readObject (com.threerings.io.ObjectInputStream ins)
            throws java.io.IOException, java.lang.ClassNotFoundException
        {
            com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.PosterInfo.RankGroup.class, "week", this, ins);
            com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.PosterInfo.RankGroup.class, "rankings", this, ins);
        }

        // from interface Streamable
        public void writeObject (com.threerings.io.ObjectOutputStream out)
            throws java.io.IOException
        {
            com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.PosterInfo.RankGroup.class, "week", this, out);
            com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.PosterInfo.RankGroup.class, "rankings", this, out);
        }
    }

    /** The handle of this poster's player */
    public Handle handle;

    /** The name of the player's gang, or <code>null</code> for none. */
    public Handle gang;

    /** The player's rank in the gang. */
    public byte rank;

    /** The player's title in the gang. */
    public int title;

    /** The player's gang buckle. */
    public BuckleInfo buckle;

    /** This poster's player's avatar */
    public AvatarInfo avatar;

    /** The personal statement given by this poster's player */
    public String statement;

    /** The three favourite badges of this poster's player */
    public int[] badgeIds;

    /** The player's ranking in each scenario */
    public StreamableArrayList<RankGroup> rankGroups = new StreamableArrayList<RankGroup>();

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return handle;
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.PosterInfo.class, "handle", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.PosterInfo.class, "gang", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.PosterInfo.class, "rank", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.PosterInfo.class, "title", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.PosterInfo.class, "buckle", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.PosterInfo.class, "avatar", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.PosterInfo.class, "statement", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.PosterInfo.class, "badgeIds", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.PosterInfo.class, "rankGroups", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.PosterInfo.class, "handle", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.PosterInfo.class, "gang", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.PosterInfo.class, "rank", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.PosterInfo.class, "title", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.PosterInfo.class, "buckle", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.PosterInfo.class, "avatar", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.PosterInfo.class, "statement", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.PosterInfo.class, "badgeIds", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.PosterInfo.class, "rankGroups", this, out);
    }
}
