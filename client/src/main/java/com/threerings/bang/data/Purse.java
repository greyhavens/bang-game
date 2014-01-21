//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;

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

    /** The bonus mulitpliers for the various purses. This is public so that we can use this
     * information when displaying the purses for sale in the General Store. */
    public static final float[] PURSE_BONUS  = {
        1f, // default
        1.1f, // frontier town
        1.15f, // indian village
        1.20f, // boom town
        1.25f, // ghost town
        1.30f, // city of gold
    };

    /**
     * Returns the path to the icon to use for the specified purse.
     */
    public static String getIconPath (int townIndex)
    {
        return "goods/purses/" + PURSE_TYPES[townIndex] + ".png";
    }

    /**
     * Returns a qualified translatable string describing the specifid purse.
     */
    public static String getName (int townIndex)
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m." + PURSE_TYPES[townIndex]);
    }

    /**
     * Returns a description of the specified purse as a qualified translatable string.
     */
    public static String getDescrip (int townIndex)
    {
        int pct = Math.round(PURSE_BONUS[townIndex] * 100) - 100;
        String msg = MessageBundle.tcompose("m.purse_tip", String.valueOf(pct));
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    /**
     * Returns a qualified translatable string to display in a tooltip when the player is hovering
     * over the specified purse's icon.
     */
    public static String getTooltip (int townIndex)
    {
        String msg = MessageBundle.compose(
            "m.goods_icon", "m." + PURSE_TYPES[townIndex], getDescrip(townIndex));
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

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

    @Override // from Item
    public String getName ()
    {
        return getName(_townIndex);
    }

    @Override // from Item
    public String getTooltip (PlayerObject user)
    {
        return getTooltip(_townIndex);
    }

    @Override // from Item
    public String getIconPath ()
    {
        return getIconPath(_townIndex);
    }

    @Override // from Item
    public boolean isEquivalent (Item other)
    {
        return super.isEquivalent(other) && ((Purse)other)._townIndex == _townIndex;
    }

    protected int _townIndex;
}
