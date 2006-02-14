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

    /** The bonus mulitpliers for the various purses. This is public so that we
     * can use this information when displaying the purses for sale in the
     * General Store. */
    public static final float[] PURSE_BONUS  = {
        1f, // default
        1.1f, // frontier town
        1.2f, // indian village
        1.3f, // boom town
        1.4f, // ghost town
        1.5f, // city of gold
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
     * Returns a "bonus" multiplier for awarded cash due to this purse.
     */
    public float getPurseBonus ()
    {
        return PURSE_BONUS[_townIndex];
    }

    @Override // documentation inherited
    public ItemIcon createIcon ()
    {
        return new PurseIcon();
    }

    protected int _townIndex;
}
