//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.util.MessageBundle;

/**
 * Codes and constants.
 */
public interface BangCodes extends InvocationCodes
{
    /** A city code. */
    public static final String FRONTIER_TOWN = "frontier_town";

    /** A city code. */
    public static final String INDIAN_POST = "indian_post";

    /** A city code. */
    public static final String BOOM_TOWN = "boom_town";

    /** A city code. */
    public static final String GHOST_TOWN = "ghost_town";

    /** A city code. */
    public static final String CITY_OF_GOLD = "city_of_gold";

    /** Enumerates our various town ids in order of accessibility. */
    public static final String[] TOWN_IDS = {
        FRONTIER_TOWN, INDIAN_POST, BOOM_TOWN, /* GHOST_TOWN, CITY_OF_GOLD */
    };

    /** The message bundle identifier for our translation messages. */
    public static final String BANG_MSGS = "bang";

    /** The message bundle identifier for options translation messages. */
    public static final String OPTS_MSGS = "options";

    /** The message bundle identifier for chat-related translation messages. */
    public static final String CHAT_MSGS = "chat";

    /** The message bundle identifier for our translation messages. */
    public static final String STATS_MSGS = "stats";

    /** The message bundle identifier for our translation messages. */
    public static final String BADGE_MSGS = "badge";

    /** The message bundle identifier for our translation messages. */
    public static final String UNITS_MSGS = "units";

    /** The message bundle identifier for our translation messages. */
    public static final String GOODS_MSGS = "goods";

    /** The message bundle identifier for our translation messages. */
    public static final String CARDS_MSGS = "cards";

    /** The message bundle identifier for our translation messages. */
    public static final String TOURNEY_MSGS = "tourney";

    /** The last player id that is considered a beta player. */
    public static final int BETA_PLAYER_CUTOFF = 33755;

    /** The number of offers of each type we publish in the coin exchange. */
    public static final int COINEX_OFFERS_SHOWN = 3;

    /** The maximum number of pardners you can have. */
    public static final int MAX_PARDNERS = 75;

    /** The layer for popups that should never be auto cleared and should be
     * hovered above everything (like the bug report popup). */
    public static final int NEVER_CLEAR_LAYER = 10;

    /** An error code when the user needs to create a handle. */
    public static final String E_CREATE_HANDLE = "e.create_handle";

    /** An error code when the user needs to create an account. */
    public static final String E_SIGN_UP = "e.sign_up";

    /** An error code when the user needs to verify they are over 13. */
    public static final String E_UNDER_13 = "e.under_13";

    /** An error code reported when a financial transaction cannot complete. */
    public static final String E_INSUFFICIENT_SCRIP =
        MessageBundle.qualify(BANG_MSGS, "e.insufficient_scrip");

    /** An error code reported when a financial transaction cannot complete. */
    public static final String E_INSUFFICIENT_COINS =
        MessageBundle.qualify(BANG_MSGS, "e.insufficient_coins");

    /** An error code reported when a financial transaction cannot complete. */
    public static final String E_INSUFFICIENT_ACES =
        MessageBundle.qualify(BANG_MSGS, "e.insufficient_aces");

    /** An error code reported when a financial transaction cannot complete. */
    public static final String E_LACK_ONETIME = MessageBundle.qualify(BANG_MSGS, "e.lack_onetime");

    /** An error code reported when the player in question does not exist. This message must be
     * accompanied by the handle of the requested player as its first argument. */
    public static final String E_NO_SUCH_PLAYER =
        MessageBundle.qualify(BANG_MSGS, "e.no_such_player");

    /** An error code reported when the action is already in progress. */
    public static final String E_IN_PROGRESS = MessageBundle.qualify(BANG_MSGS, "e.in_progress");

    /** The minimum age of a player to avoid COPPA regulations. */
    public static final int COPPA_YEAR = 13;
}
