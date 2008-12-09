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
}
