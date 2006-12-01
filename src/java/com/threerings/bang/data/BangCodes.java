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
        FRONTIER_TOWN, INDIAN_POST /*, BOOM_TOWN, GHOST_TOWN, CITY_OF_GOLD */
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

    /** The last player id that is considered a beta player. */
    public static final int BETA_PLAYER_CUTOFF = 33765;

    /** The number of offers of each type we publish in the coin exchange. */
    public static final int COINEX_OFFERS_SHOWN = 3;

    /** The maximum number of pardners you can have. */
    public static final int MAX_PARDNERS = 75;

    /** The layer for popups that should never be auto cleared and should be
     * hovered above everything (like the bug report popup). */
    public static final int NEVER_CLEAR_LAYER = 10;

    /** An error code reported when a financial transaction cannot complete. */
    public static final String INSUFFICIENT_FUNDS =
        MessageBundle.qualify(BANG_MSGS, "e.insufficient_funds");
}
