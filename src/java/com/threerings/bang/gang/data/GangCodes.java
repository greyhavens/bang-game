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

    /** The rating type identifier used for gang notoriety. */
    public static final String NOTORIETY_IDENT = "no";

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
        "m.member", "m.recruiter", "m.leader" };

    /** The cost of forming a gang in scrip. */
    public static final int FORM_GANG_SCRIP_COST = 2500;

    /** The cost of a forming a gang in coins. */
    public static final int FORM_GANG_COIN_COST = 5;

    /** The amount of time that may elapse before gang members are considered to be inactive. */
    public static final long ACTIVITY_DELAY = 14L * 24 * 60 * 60 * 1000;

    /** The amount of time that must elapse before members can contribute to the gang's coffers. */
    public static final long DONATION_DELAY = 7L * 24 * 60 * 60 * 1000;
}
