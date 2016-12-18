//
// $Id$

package com.threerings.bang.store.data;

import com.samskivert.util.StringUtil;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Star;

/**
 * Used to sell Deputy's Stars.
 */
public class StarGood extends Good
{
    /** All Stars cost the same amount in coins. */
    public static final int COIN_COST = 4;

    /** All Stars cost a linearly increasing amount in scrip. */
    public static final int SCRIP_COST = 1000;

    /**
     * Creates a good representing a pass for the specified unit.
     */
    public StarGood (int townIdx, Star.Difficulty difficulty)
    {
        super("star_" + BangCodes.TOWN_IDS[townIdx] + "_" +
                StringUtil.toUSLowerCase(difficulty.toString()), BangCodes.TOWN_IDS[townIdx],
              difficulty.ordinal() * SCRIP_COST, COIN_COST, STAR_PRIORITY);
        _townIdx = townIdx;
        _difficulty = difficulty;
    }

    /** A constructor only used during serialization. */
    public StarGood ()
    {
    }

    @Override // documentation inherited
    public String getName ()
    {
        return Star.getName(_townIdx, _difficulty);
    }

    @Override // documentation inherited
    public String getTip ()
    {
        return Star.getTooltip(_townIdx, _difficulty);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return Star.getIconPath(_townIdx, _difficulty);
    }

    @Override // from Good
    public boolean honorsGoldPass ()
    {
        return true;
    }

    @Override // documentation inherited
    public boolean isAvailable (PlayerObject user)
    {
        // make sure we don't already have it
        if (user.holdsStar(_townIdx, _difficulty)) {
            return false;
        }

        // otherwise if we're medium or below, we're available by default
        switch (_difficulty) {
        default: return false;
        case EASY:
        case MEDIUM: return true;
        case HARD:
        case EXTREME: return user.holdsStar(_townIdx, Star.getPrevious(_difficulty));
        }
    }

    @Override // documentation inherited
    public Item createItem (int playerId)
    {
        return new Star(playerId, _townIdx, _difficulty);
    }

    protected int _townIdx;
    protected Star.Difficulty _difficulty;

    protected static final int STAR_PRIORITY = 20;
}
