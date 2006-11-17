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
    
    /** The cost of forming a gang in scrip. */
    public static final int FORM_GANG_SCRIP_COST = 2500;

    /** The cost of a forming a gang in coins. */
    public static final int FORM_GANG_COIN_COST = 5;
}
