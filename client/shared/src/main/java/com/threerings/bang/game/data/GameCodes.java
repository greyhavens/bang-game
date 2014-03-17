//
// $Id$

package com.threerings.bang.game.data;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.bang.util.DeploymentConfig;
import com.threerings.bang.game.client.BangService;

/**
 * Codes and constants used by the actual game.
 */
public interface GameCodes extends InvocationCodes
{
    /** The message bundle identifier for our translation messages. */
    public static final String GAME_MSGS = "game";

    /** The number of milliseconds allowed to make a team selection. */
    public static final long SELECT_TIMEOUT = 60 * 1000L;

    /** The number of milliseconds allowed to view stats between rounds. */
    public static final long STATS_TIMEOUT = 30 * 1000L;

    /** A response code for {@link BangService#order}. */
    public static final Integer EXECUTED_ORDER = 0;

    /** A response code for {@link BangService#order}. */
    public static final Integer QUEUED_ORDER = 1;

    /** An error response code for {@link BangService#order}. */
    public static final String MOVER_NO_LONGER_VALID = "m.mover_invalid";

    /** An error response code for {@link BangService#order}. */
    public static final String MOVE_BLOCKED = "m.move_blocked";

    /** An error response code for {@link BangService#order}. */
    public static final String TARGET_NO_LONGER_VALID = "m.target_invalid";

    /** An error response code for {@link BangService#order}. */
    public static final String TARGET_UNREACHABLE = "m.target_unreachable";

    /** An error response code for {@link BangService#order}. */
    public static final String GAME_ENDED = "m.game_ended";

    /** An error response code for {@link BangService#playCard}. */
    public static final String CARD_UNPLAYABLE = "m.card_unplayable";

    /** An feedback message for {@link BangService#cancelOrder}. */
    public static final String ORDER_CLEARED = "m.order_cleared";

    /** The highest number of players we will allow in a game. */
    public static final int MAX_PLAYERS = 4;

    /** The highest number of rounds we will allow in a game. */
    public static final int MAX_ROUNDS = 3;

    /** Defines the minimum team size (not counting one's big shot). */
    public static final int MIN_TEAM_SIZE = 2;

    /** Defines the maximum team size (not counting one's big shot). */
    public static final int MAX_TEAM_SIZE = 5;

    /** The maximum number of cards a player can hold in a game. */
    public static final int MAX_CARDS = 3;

    /** Set to true to compile in sync debugging mode. */
    public static final boolean SYNC_DEBUG = false &&
        (DeploymentConfig.getVersion() == 0);
}
