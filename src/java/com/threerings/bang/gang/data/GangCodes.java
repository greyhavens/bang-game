//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants related to gangs.
 */
public interface GangCodes extends InvocationCodes
{
    /** The message bundle identifier for our translation messages. */
    public static final String GANG_MSGS = "gang";
    
    /** Gang rank constant. */
    public static final byte MEMBER_RANK = 0;

    /** Gang rank constant. */
    public static final byte RECRUITER_RANK = 1;

    /** Gang rank constant. */
    public static final byte LEADER_RANK = 2;

    /** The number of gang ranks. */
    public static final byte RANK_COUNT = 3;
    
    /** Gang rank string translations. */
    public static final String[] XLATE_RANKS = {
        "member", "recruiter", "leader" };
    
    /** The cost of forming a gang in scrip. */
    public static final int FORM_GANG_SCRIP_COST = 2500;

    /** The cost of a forming a gang in coins. */
    public static final int FORM_GANG_COIN_COST = 5;
}
