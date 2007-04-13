//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants relating to the hideout services.
 */
public interface HideoutCodes extends InvocationCodes
{
    /** The identifier for our message bundle. */
    public static final String HIDEOUT_MSGS = "hideout";

    /** The maximum number of characters in a gang's statement. */
    public static final int MAX_STATEMENT_LENGTH = 200;

    /** The maximum number of characters in a gang's URL. */
    public static final int MAX_URL_LENGTH = 200;

    /** The maximum number of characters in a gang member broadcast. */
    public static final int MAX_BROADCAST_LENGTH = 200;

    /** The (maximum) number of history entries displayed on a single page. */
    public static final int HISTORY_PAGE_ENTRIES = 10;
}
