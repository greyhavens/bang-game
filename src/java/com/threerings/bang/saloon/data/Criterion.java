//
// $Id$

package com.threerings.bang.saloon.data;

import java.util.ArrayList;

import com.threerings.bang.game.data.GameCodes;
import com.threerings.io.SimpleStreamableObject;

import static com.threerings.bang.Log.log;

/**
 * Defines various criterion for finding opponents.
 */
public class Criterion extends SimpleStreamableObject
    implements Cloneable
{
    /** Controls ranking closeness when matchmaking. */
    public static final int TIGHT = 0;

    /** Controls ranking closeness when matchmaking. */
    public static final int LOOSE = 1;

    /** Controls ranking closeness when matchmaking. */
    public static final int OPEN = 2;

    /** A bitmask indicating which round counts to allow. */
    public int rounds;

    /** A bitmask indicating which player counts to allow. */
    public int players;

    /** A bitmask indicating which rankednesses to allow. */
    public int ranked;

    /** Indicates the ranking range to allow when matchmaking, one of {@link
     * #TIGHT}, {@link #LOOSE} or {@link #OPEN}. */
    public int range;

    /** A bitmask indicating how many AIs to allow. */
    public int allowAIs;

    /** Whether to allow previous towns' scenarios. */
    public boolean allowPreviousTowns = true;

    /** Utility function for creating criterion instances. */
    public static int compose (boolean val1, boolean val2, boolean val3)
    {
        return (val1 ? 1 : 0) | (val2 ? (1<<1) : 0) | (val3 ? (1<<2) : 0);
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

    /**
     * Returns true if this criterion is compatible with the specified other
     * criterion. <em>Note:</em> the {@link #range} criterion is not accounted
     * for by this method as that requires knowledge of the involved players'
     * ratings.
     *
     * @return true if the criterion are compatible false if not.
     */
    public boolean isCompatible (Criterion other)
    {
        return (rounds & other.rounds) != 0 &&
            (players & other.players) != 0 &&
            (ranked & other.ranked) != 0 &&
            (allowAIs == other.allowAIs || (allowAIs & other.allowAIs) != 0) &&
            (allowPreviousTowns || !other.allowPreviousTowns);
    }

    /**
     * Merges the supplied other criterion into this criterion. Our bitmasks
     * will be set to the intersection of the two criterion and our rating
     * range will be set to the stricter of the two ranges.
     */
    public void merge (Criterion other)
    {
        rounds &= other.rounds;
        players &= other.players;
        ranked &= other.ranked;
        range = Math.min(range, other.range);
        allowAIs &= other.allowAIs;
        allowPreviousTowns &= other.allowPreviousTowns;
    }

    /**
     * Returns true if the game could start with the specified player count.
     */
    public boolean couldStart (int playerCount)
    {
        for (int ii = 1, ll = getAllowedAIs(); ii <= ll; ii++) {
            if (isBitSet(players, playerCount-2 + ii)) {
                return true;
            }
        }
        return isBitSet(players, playerCount-2);
    }

    /**
     * Returns the largest allowed player count.
     */
    public int getDesiredPlayers ()
    {
        return highestBitIndex(players) + 1;
    }

    /**
     * Returns the highest allowed number of rounds.
     */
    public int getDesiredRounds ()
    {
        return highestBitIndex(rounds);
    }

    /**
     * Returns the desired ranked setting, favoring ranked games over unranked.
     */
    public boolean getDesiredRankedness ()
    {
        return (ranked & 1) != 0;
    }

    /**
     * Returns the largest allow AI count.
     */
    public int getAllowedAIs ()
    {
        return Math.max(0, highestBitIndex(allowAIs)-1);
    }

    /**
     * Returns a string describing the allowable player counts.
     */
    public String getPlayerString ()
    {
        ArrayList<String> values = new ArrayList<String>();
        for (int ii = 2; ii <= GameCodes.MAX_PLAYERS; ii++) {
            if (isBitSet(players, ii-2)) {
                values.add(String.valueOf(ii));
            }
        }
        return join(values);
    }

    /**
     * Returns a string describing the allowable round counts.
     */
    public String getRoundString ()
    {
        ArrayList<String> values = new ArrayList<String>();
        for (int ii = 1; ii <= GameCodes.MAX_ROUNDS; ii++) {
            if (isBitSet(rounds, ii-1)) {
                values.add(String.valueOf(ii));
            }
        }
        return join(values);
    }

    /**
     * Returns a string describing the allowable AI counts.
     */
    public String getAIString ()
    {
        ArrayList<String> values = new ArrayList<String>();
        for (int ii = 0; ii < GameCodes.MAX_PLAYERS-1; ii++) {
            if (isBitSet(allowAIs, ii)) {
                values.add(String.valueOf(ii));
            }
        }
        return values.size() > 0 ? join(values) : Integer.toString(0);
    }

    protected boolean isBitSet (int mask, int bit)
    {
        return (mask & (1 << bit)) != 0;
    }

    protected int highestBitIndex (int bitmask)
    {
        int highest = 0;
        while (bitmask > 0) {
            bitmask >>= 1;
            highest++;
        }
        return highest;
    }

    /**
     * Special join function that assumes we only have three possible values.
     */
    protected String join (ArrayList<String> values)
    {
        switch (values.size()) {
        case 1: return values.get(0);
        case 2: return values.get(0) + ", " + values.get(1);
        case 3: return values.get(0) + "-" + values.get(2);
        default:
            StringBuffer buf = new StringBuffer();
            for (String value : values) {
                if (buf.length() > 0) {
                    buf.append(",");
                }
                buf.append(value);
            }
            return buf.toString();
        }
    }
}
