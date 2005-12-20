//
// $Id$

package com.threerings.bang.game.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants used by the actual game.
 */
public interface GameCodes extends InvocationCodes
{
    /** The message bundle identifier for our translation messages. */
    public static final String GAME_MSGS = "game";

    /** An error message delivered when a move fails due to another piece
     * blocking. */
    public static final String MOVE_BLOCKED = "m.move_blocked";

    /** An error message delivered when a shot fails due the target having
     * moved out of range. */
    public static final String TARGET_MOVED = "m.target_moved";

    /** An error message delivered when a shot fails due the target having
     * become invalid (by dying for example). */
    public static final String TARGET_NO_LONGER_VALID =
        "m.target_no_longer_valid";

    /** Defines the minimum team size (not counting one's big shot). */
    public static final int MIN_TEAM_SIZE = 2;

    /** Defines the maximum team size (not counting one's big shot). */
    public static final int MAX_TEAM_SIZE = 6;

    /** The maximum number of cards a player can hold in a game. */
    public static final int MAX_CARDS = 3;

    /** The code for the tutorial scenario. */
    public static final String TUTORIAL = "tu";

    /** The code for the claim jumping scenario. */
    public static final String CLAIM_JUMPING = "cj";

    /** The code for the cattle herding scenario. */
    public static final String CATTLE_HERDING = "ch";

    /** The amount of cash earned per nugget at the end of the game. */
    public static final int CASH_PER_NUGGET = 50;

    /** Cash earned for each branded cow. */
    public static final int CASH_PER_COW = 50;
}
