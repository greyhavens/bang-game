//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants.
 */
public interface BangCodes extends InvocationCodes
{
    /** The message bundle identifier for our translation messages. */
    public static final String BANG_MSGS = "bang";

    /** An error message delivered when a move fails due to another piece
     * blocking. */
    public static final String MOVE_BLOCKED = "m.move_blocked";

    /** An error message delivered when a shot fails due the target having
     * moved out of range. */
    public static final String TARGET_MOVED = "m.target_moved";
}
