//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.util.MessageBundle;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants relating to the Saloon services.
 */
public interface SaloonCodes extends InvocationCodes
{
    /** The identifier for our message bundle. */
    public static final String SALOON_MSGS = "saloon";

    /** An error code used by the back parlor services. */
    public static final String ALREADY_HAVE_PARLOR = "m.already_have_parlor";

    /** An error code used by the back parlor services. */
    public static final String NO_SUCH_PARLOR = "m.no_such_parlor";

    /** An error code used by the back parlor services. */
    public static final String INCORRECT_PASSWORD = "m.incorrect_password";

    /** An error code used by the back parlor services. */
    public static final String NOT_PARDNER = "m.not_pardner";

    /** An error code used by the back parlor services. */
    public static final String CREATOR_NOT_ONLINE = "m.creator_not_online";

    /** An error code used by the back parlor services. */
    public static final String BOOTED = "m.booted";

    /** An error code reported when new games are not allowed. */
    public static final String NEW_GAMES_DISABLED =
        MessageBundle.qualify(SALOON_MSGS, "m.new_games_disabled");

    /** An error code reported when trying to start a new game when not the creator. */
    public static final String CREATOR_ONLY =
        MessageBundle.qualify(SALOON_MSGS, "m.creator_only");
}
