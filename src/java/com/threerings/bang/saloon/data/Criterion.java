//
// $Id$

package com.threerings.bang.saloon.data;

import java.util.ArrayList;

import com.threerings.bang.game.data.GameCodes;
import com.threerings.io.SimpleStreamableObject;

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

    /** Controls the game mode. */
    public static final int ANY = 0;
    public static final int COMP = 1;
    public static final int TEAM_2V2 = 2; // Not currently supported
    public static final int COOP = 3;

    /** A bitmask indicating which round counts to allow. */
    public int rounds;

    /** A bitmask indicating which player counts to allow. */
    public int players;

    /** Indicates the ranking range to allow when matchmaking, one of {@link
     * #TIGHT}, {@link #LOOSE} or {@link #OPEN}. */
    public int range;

    /** Indicates the game mode when matchmaking, one of {@link #ANY}, {@link #COMP},
     * {@link #TEAM_2V2} or {@link #COOP}. */
    public int mode;

    /** Indicated the game must be a gang match. */
    public boolean gang;

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
            (mode == other.mode || mode == ANY || other.mode == ANY);
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
        range = Math.min(range, other.range);
        allowPreviousTowns &= other.allowPreviousTowns;
        gang &= other.gang;
        mode = Math.max(mode, other.mode);
    }

    /**
     * Returns true if the game could start with the specified player count.
     */
    public boolean couldStart (int playerCount)
    {
        return playerCount > 1 && isBitSet(players, playerCount-2);
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
     * Returns the translation key for the game mode.
     */
    public String getModeString ()
    {
        switch (mode) {
        case ANY:
            return "m.any";
        case COOP:
            return "m.coop";
        default:
            return "m.comp";
        }
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
