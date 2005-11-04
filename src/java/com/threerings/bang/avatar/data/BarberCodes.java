//
// $Id$

package com.threerings.bang.avatar.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants relating to the Barber services.
 */
public interface BarberCodes extends InvocationCodes
{
    /** The identifier for our message bundle. */
    public static final String BARBER_MSGS = "barber";

    /** The maximum length of a "look" name. */
    public static final int MAX_LOOK_NAME_LENGTH = 48;

    /** The base scrip cost for a new look. */
    public static final int BASE_LOOK_SCRIP_COST = 1000;

    /** The base coin cost for a new look. */
    public static final int BASE_LOOK_COIN_COST = 2;
}
