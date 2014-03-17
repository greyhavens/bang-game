//
// $Id$

package com.threerings.bang.station.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants relating to the Train Station services.
 */
public interface StationCodes extends InvocationCodes
{
    /** The identifier for our message bundle. */
    public static final String STATION_MSGS = "station";

    /** The cost of our train tickets in scrip. */
    public static final int[] TICKET_SCRIP = {
        0, // frontier_town
        1000, // indian_post
        2000, // boom_town
        3500, // ghost_town
        5000, // city_of_gold
    };

    /** The cost of our train tickets in coins. */
    public static final int[] TICKET_COINS = {
        0, // frontier_town
        20, // indian_post
        -1, // 20, // boom_town (uncomment to enable sale)
        -1, // 20, // ghost_town
        -1, // 20, // city_of_gold
    };
}
