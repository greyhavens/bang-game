//
// $Id$

package com.threerings.bang.data;

import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.PurseIcon;

/**
 * Enables a player to retain more money (per round) at the end of a game.
 */
public class Purse extends Item
{
    /** The default purse, used when a player doesn't yet have one. */
    public static final Purse DEFAULT_PURSE = new Purse(-1, 0);

    /** Provides text identifiers for the various purse types. */
    public static final String[] PURSE_TYPES = {
        "default", // not used
        "frontier_purse",
        "indian_purse",
        "boom_purse",
        "ghost_purse",
        "gold_purse",
    };

    /** The per-round-cash earned by the various purses. This is public so
     * that we can use this information when displaying the purses for
     * sale in the General Store. */
    public static final int[] PER_ROUND_CASH  = {
        50, // default
        60, // frontier town
        70, // indian village
        85, // boom town
        100, // ghost town
        120, // city of gold
    };

    /** A default constructor used for serialization. */
    public Purse ()
    {
    }

    /**
     * Creates the purse associated with the specified town.
     */
    public Purse (int ownerId, int townIndex)
    {
        super(ownerId);
        _townIndex = townIndex;
    }

    /**
     * Returns the index of the town with which this purse is associated.
     */
    public int getTownIndex ()
    {
        return _townIndex;
    }

    /**
     * Returns the amount of money that a player holding this purse may
     * retain (per round) from a game.
     */
    public int getPerRoundCash ()
    {
        return PER_ROUND_CASH[_townIndex];
    }

    @Override // documentation inherited
    public ItemIcon createIcon ()
    {
        return new PurseIcon();
    }

    protected int _townIndex;
}
