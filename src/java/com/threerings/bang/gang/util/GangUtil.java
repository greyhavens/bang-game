//
// $Id$

package com.threerings.bang.gang.util;

import java.util.ArrayList;
import java.util.Comparator;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.QuickSort;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Item;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangMemberEntry;

import static com.threerings.bang.Log.log;

/**
 * Gang-related utility methods and classes.
 */
public class GangUtil
{
    /**
     * Returns the senior active leader among the described gang members, or <code>null</code> if
     * there aren't any active leaders.
     */
    public static GangMemberEntry getSeniorLeader (Iterable<GangMemberEntry> members)
    {
        GangMemberEntry senior = null;
        for (GangMemberEntry entry : members) {
            if (entry.rank == GangCodes.LEADER_RANK && entry.isActive() &&
                (senior == null || entry.joined < senior.joined)) {
                senior = entry;
            }
        }
        return senior;
    }

    /**
     * Returns a list containing either the leaders or the normal members of the gang.  Leaders
     * are sorted by decreasing seniority, members by decreasing notoriety.
     */
    public static ArrayList<GangMemberEntry> getSortedMembers (
        Iterable<GangMemberEntry> members, boolean leaders)
    {
        ArrayList<GangMemberEntry> entries = new ArrayList<GangMemberEntry>();
        for (GangMemberEntry entry : members) {
            if ((entry.rank == GangCodes.LEADER_RANK) == leaders) {
                entries.add(entry);
            }
        }
        QuickSort.sort(entries, leaders ? LEADER_COMP : MEMBER_COMP);
        return entries;
    }

    /**
     * Forms and returns a buckle fingerprint based on the ordered list of buckle part ids and
     * the item set supplied.
     */
    public static <T extends Item> BuckleInfo getBuckleInfo (int[] partIds, DSet<T> items)
    {
        // make sure it exists
        if (partIds == null || partIds.length == 0) {
            return null;
        }

        // find the listed parts
        BucklePart[] parts = new BucklePart[partIds.length];
        for (int ii = 0; ii < parts.length; ii++) {
            Item item = items.get(partIds[ii]);
            if (!(item instanceof BucklePart)) {
                log.warning("Invalid part in buckle [item=" + item + "].");
                return null;
            }
            parts[ii] = (BucklePart)item;
        }

        // put them into the print in order, with each component having an encoded component id
        // and colorization followed by encoded coordinates
        int[] print = new int[0], pair = new int[2];
        for (BucklePart part : parts) {
            pair[1] = (part.getX() << 16) | (part.getY() & 0xFFFF);
            for (int comp : part.getComponents()) {
                pair[0] = comp;
                print = ArrayUtil.concatenate(print, pair);
            }
        }
        return new BuckleInfo(print);
    }

    /** Sorts active members before inactive, then by decreasing seniority. */
    protected static final Comparator<GangMemberEntry> LEADER_COMP =
        new Comparator<GangMemberEntry>() {
            public int compare (GangMemberEntry m1, GangMemberEntry m2) {
                if (m1.isActive() != m2.isActive()) {
                    return (m1.isActive() ? -1 : +1);
                }
                long diff = m1.joined - m2.joined;
                return (diff == 0) ? 0 : (diff < 0 ? -1 : +1);
            }
        };

    /** Sorts active members before inactive, then by decreasing notoriety. */
    protected static final Comparator<GangMemberEntry> MEMBER_COMP =
        new Comparator<GangMemberEntry>() {
            public int compare (GangMemberEntry m1, GangMemberEntry m2) {
                if (m1.isActive() != m2.isActive()) {
                    return (m1.isActive() ? -1 : +1);
                }
                return m2.notoriety - m1.notoriety;
            }
        };
}
