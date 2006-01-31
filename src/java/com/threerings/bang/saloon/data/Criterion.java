//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Defines various criterion for finding opponents.
 */
public class Criterion extends SimpleStreamableObject
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

    /** Utility function for creating criterion instances. */
    public static int compose (boolean val1, boolean val2, boolean val3)
    {
        return (val1 ? 1 : 0) | (val2 ? (1<<1) : 0) | (val3 ? (1<<2) : 0);
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
            (ranked & other.ranked) != 0;
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
    }

    /**
     * Returns true if the specified player count is allowed.
     */
    public boolean isValidPlayerCount (int playerCount)
    {
        return (players & (1 << (playerCount-2))) != 0;
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
        return (ranked & (1|(1<<2))) != 0;
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
}
