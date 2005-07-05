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

    /** An error code reported when a financial transaction cannot complete. */
    public static final String INSUFFICIENT_FUNDS =
        MessageBundle.qualify(BANG_MSGS, "e.insufficient_funds");
}
