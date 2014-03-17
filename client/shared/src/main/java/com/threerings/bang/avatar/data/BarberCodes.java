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
    public static final int MAX_LOOK_NAME_LENGTH = 24;

    /** The cost of a handle change in scrip. */
    public static final int HANDLE_CHANGE_SCRIP_COST = 1000;

    /** The cost of a handle change in coins. */
    public static final int HANDLE_CHANGE_COIN_COST = 4;
}
