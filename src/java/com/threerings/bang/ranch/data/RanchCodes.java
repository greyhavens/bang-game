//
// $Id$

package com.threerings.bang.ranch.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants relating to the ranch services.
 */
public interface RanchCodes extends InvocationCodes
{
    /** The identifier for our message bundle. */
    public static final String RANCH_MSGS = "ranch";

    /** Big Shots given out to new players. */
    public static final String[] STARTER_BIGSHOTS = {
        "frontier_town/cavalry", "frontier_town/codger",
        "frontier_town/tactician"
    };
}
