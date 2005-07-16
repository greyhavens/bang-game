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

    /** Defines the maximum number of units (not counting ones big shot)
     * that can be recruited at the start of a round. */
    public static final int MAX_TEAM_SIZE = 6;
}
