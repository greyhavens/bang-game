//
// $Id$

package com.threerings.bang.gang.util;

import java.util.ArrayList;
import java.util.Comparator;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.QuickSort;

import com.threerings.presents.dobj.DSet;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.WeightClassUpgrade;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangMemberEntry;

import static com.threerings.bang.Log.log;

/**
 * Gang-related utility methods and classes.
 */
public class GangUtil
    implements GangCodes
{
    /**
     * Determines a gang's weight class by running through its inventory looking for
     * upgrades.
     */
    public static byte getWeightClass (Iterable<Item> inventory)
    {
        int weightClass = 0;
        for (Item item : inventory) {
            if (item instanceof WeightClassUpgrade) {
                weightClass = Math.max(weightClass, ((WeightClassUpgrade)item).getWeightClass());
            }
        }
        return (byte)weightClass;
    }

    /**
     * Given a gang's weight class and raw notoriety, returns its notoriety level.
     */
    public static byte getNotorietyLevel (int wclass, int notoriety)
    {
        int[] cutoffs = WEIGHT_CLASSES[wclass].notorietyLevels;
        for (byte ii = 0; ii < cutoffs.length; ii++) {
            if (notoriety < cutoffs[ii]) {
                return ii;
            }
        }
        return (byte)cutoffs.length;
    }

    /**
     * Returns the senior active leader among the described gang members, or <code>null</code> if
     * there aren't any active leaders.
     */
    public static GangMemberEntry getSeniorLeader (Iterable<GangMemberEntry> members)
    {
        GangMemberEntry senior = null;
        for (GangMemberEntry entry : members) {
            if (entry.rank == LEADER_RANK && entry.isActive() &&
                (senior == null || entry.commandOrder < senior.commandOrder)) {
                senior = entry;
            }
        }
        return senior;
    }

    /**
     * Returns a list containing either the leaders or the normal members of the gang.  Leaders
     * are sorted by increasing command order, members by decreasing notoriety (then by decreasing
     * seniority).
     *
     * @param online if true, sort online members before offline ones.
     */
    public static ArrayList<GangMemberEntry> getSortedMembers (
        Iterable<GangMemberEntry> members, final boolean online, final boolean leaders)
    {
        ArrayList<GangMemberEntry> entries = new ArrayList<GangMemberEntry>();
        for (GangMemberEntry entry : members) {
            if ((entry.rank == LEADER_RANK) == leaders) {
                entries.add(entry);
            }
        }
        QuickSort.sort(entries, new Comparator<GangMemberEntry>() {
            public int compare (GangMemberEntry m1, GangMemberEntry m2) {
                if (m1.isActive() != m2.isActive()) {
                    return (m1.isActive() ? -1 : +1);
                }
                if (online && (m1.isOnline() != m2.isOnline())) {
                    return (m1.isOnline() ? -1 : +1);
                }
                if (leaders) {
                    return m1.commandOrder - m2.commandOrder;
                } else {
                    int ndiff = m2.notoriety - m1.notoriety;
                    if (ndiff != 0) {
                        return ndiff;
                    }
                    long jdiff = m1.joined - m2.joined;
                    return (jdiff == 0) ? 0 : (jdiff < 0 ? -1 : +1);
                }
            }
        });
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

        // find the listed parts and use them to form the fingerprint
        BucklePart[] parts = new BucklePart[partIds.length];
        for (int ii = 0; ii < parts.length; ii++) {
            Item item = items.get(partIds[ii]);
            if (!(item instanceof BucklePart)) {
                log.warning("Invalid part in buckle", "partId", partIds[ii], "item", item);
                return null;
            }
            parts[ii] = (BucklePart)item;
        }
        return getBuckleInfo(parts);
    }

    /**
     * Forms and returns a buckle fingerprint based on the ordered list of buckle parts given.
     */
    public static BuckleInfo getBuckleInfo (BucklePart[] parts)
    {
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

    /**
     * Returns a translatable string describing the identified amounts.
     */
    public static String getMoneyDesc (int scrip, int coins, int aces)
    {
        ArrayList<String> descs = new ArrayList<String>();
        if (scrip > 0 || (scrip == 0 && coins == 0 && aces == 0)) {
            descs.add(MessageBundle.tcompose("m.scrip", String.valueOf(scrip)));
        }
        if (coins > 0) {
            descs.add(MessageBundle.tcompose("m.coins", coins));
        }
        if (aces > 0) {
            descs.add(MessageBundle.tcompose("m.aces", aces));
        }
        int ndescs = descs.size();
        if (ndescs == 1) {
            return descs.get(0);
        } else {
            return MessageBundle.compose("m.times_" + ndescs, descs.toArray(new String[ndescs]));
        }
    }

    /**
     * Puts a statement in quotes, substituting "..." if it's empty and running it through the
     * chat filter if not.
     */
    public static String quoteStatement (BangContext ctx, String statement, boolean crop)
    {
        String fstmt = statement.trim();
        if (fstmt.length() == 0) {
            fstmt = "...";
        } else if (crop && fstmt.length() > 120) {
            fstmt = fstmt.substring(0, 120) + "...";
        }
        return "\"" + ctx.getChatDirector().filter(fstmt, null, false) + "\"";
    }
}
