//
// $Id$

package com.threerings.bang.data;

import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

/**
 * Enables access to Most Wanted bounties of a particular difficulty level.
 */
public class Star extends Item
{
    /** Defines the difficulty levels. */
    public enum Difficulty { EASY, MEDIUM, HARD, EXTREME };

    /**
     * Returns a fully qualified translatable name for the specified difficulty level.
     */
    public static String getName (Difficulty difficulty)
    {
        return MessageBundle.qualify(
            BangCodes.GOODS_MSGS, "m.star_" + StringUtil.toUSLowerCase(difficulty.toString()));
    }

    /**
     * Returns a fully qualified translatable name for the specified Star configuration.
     */
    public static String getName (int townIdx, Difficulty difficulty)
    {
        return getMessage("m.star_name", BangCodes.TOWN_IDS[townIdx], difficulty);
    }

    /**
     * Returns a fully qualified translatable tooltip for the specified Star configuration.
     */
    public static String getTooltip (int townIdx, Difficulty difficulty)
    {
        return getMessage("m.star_tip", BangCodes.TOWN_IDS[townIdx], difficulty);
    }

    /**
     * Returns a path to the icon for the specified Star configuration.
     */
    public static String getIconPath (int townIdx, Difficulty difficulty)
    {
        return "goods/" + BangCodes.TOWN_IDS[townIdx] + "/stars/" +
            StringUtil.toUSLowerCase(difficulty.toString()) + ".png";
    }

    /**
     * Returns the difficulty level previous to the supplied level or null if EASY is supplied.
     */
    public static Difficulty getPrevious (Difficulty difficulty)
    {
        switch (difficulty) {
        default:
        case EASY: return null;
        case MEDIUM: return Difficulty.EASY;
        case HARD: return Difficulty.MEDIUM;
        case EXTREME: return Difficulty.HARD;
        }
    }

    /**
     * Creates a Deputy's Star for the specified town and difficulty combination.
     */
    public Star (int ownerId, int townIdx, Difficulty difficulty)
    {
        super(ownerId);
        _townIdx = townIdx;
        _difficulty = difficulty;
    }

    /** Zero argument constructor used during unserialization. */
    public Star ()
    {
    }

    /**
     * Returns the town index for which this star is applicable.
     */
    public int getTownIndex ()
    {
        return _townIdx;
    }

    /**
     * Returns the town for which this star is applicable.
     */
    public String getTownId ()
    {
        return BangCodes.TOWN_IDS[_townIdx];
    }

    /**
     * Returns the bounty difficulty level for which this star is applicable.
     */
    public Difficulty getDifficulty ()
    {
        return _difficulty;
    }

    @Override // from Item
    public String getName ()
    {
        return getName(_townIdx, _difficulty);
    }

    @Override // from Item
    public String getTooltip (PlayerObject user)
    {
        return getTooltip(_townIdx, _difficulty);
    }

    @Override // from Item
    public String getIconPath ()
    {
        return getIconPath(_townIdx, _difficulty);
    }

    @Override // from Item
    public boolean isEquivalent (Item other)
    {
        return super.isEquivalent(other) && ((Star)other)._townIdx == _townIdx &&
            ((Star)other)._difficulty == _difficulty;
    }

    @Override // from Item
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", town=").append(getTownId());
        buf.append(", diff=").append(_difficulty);
    }

    protected static String getMessage (String which, String townId, Difficulty difficulty)
    {
        String town = MessageBundle.qualify(BangCodes.BANG_MSGS, "m." + townId);
        return MessageBundle.qualify(
            BangCodes.GOODS_MSGS, MessageBundle.compose(which, getName(difficulty), town));
    }

    protected int _townIdx;
    protected Difficulty _difficulty;
}
