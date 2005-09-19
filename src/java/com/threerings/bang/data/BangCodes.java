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
    public static final String INDIAN_VILLAGE = "indian_village";

    /** A city code. */
    public static final String BOOM_TOWN = "boom_town";

    /** A city code. */
    public static final String GHOST_TOWN = "ghost_town";

    /** A city code. */
    public static final String CITY_OF_GOLD = "city_of_gold";

    /** Enumerates our various town ids in order of accessibility. */
    public static final String[] TOWN_IDS = {
        FRONTIER_TOWN, INDIAN_VILLAGE, BOOM_TOWN, GHOST_TOWN, CITY_OF_GOLD
    };

    /** The message bundle identifier for our translation messages. */
    public static final String BANG_MSGS = "bang";

    /** The message bundle identifier for chat-related translation messages. */
    public static final String CHAT_MSGS = "chat";

    /** The message bundle identifier for our translation messages. */
    public static final String STATS_MSGS = "stats";

    /** The message bundle identifier for our translation messages. */
    public static final String BADGE_MSGS = "badge";

    /** The message bundle identifier for our translation messages. */
    public static final String GOODS_MSGS = "goods";

    /** An error code reported when a financial transaction cannot complete. */
    public static final String INSUFFICIENT_FUNDS =
        MessageBundle.qualify(BANG_MSGS, "e.insufficient_funds");

    /** The highest number of players we will allow in a game (currently
     * we only support four but we'll probably get crazy and try larger
     * numbers at some point). */
    public static final int MAX_PLAYERS = 8;
}
