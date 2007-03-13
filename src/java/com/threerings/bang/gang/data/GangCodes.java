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
        "m.member", "m.recruiter", "m.leader" };

    /** The cost of forming a gang in scrip. */
    public static final int FORM_GANG_SCRIP_COST = 2500;

    /** The cost of a forming a gang in coins. */
    public static final int FORM_GANG_COIN_COST = 5;

    /** The amount of time that may elapse before gang members are considered to be inactive. */
    public static final long ACTIVITY_DELAY = 14L * 24 * 60 * 60 * 1000;

    /** The amount of time that must elapse before members can contribute to the gang's coffers. */
    public static final long DONATION_DELAY = 7L * 24 * 60 * 60 * 1000;

    /** The starting number of icons gangs can have on their buckles. */
    public static final int BASE_MAX_BUCKLE_ICONS = 3;

    /** The number of gang weight classes. */
    public static final int WEIGHT_CLASS_COUNT = 5;

    /** The maximum number of members for each of the weight classes. */
    public static final int[] MEMBER_LIMITS = { 20, 50, 100, 200, Integer.MAX_VALUE };

    /** Notoriety level cutoffs for each of the weight classes. */
    public static final int[][] NOTORIETY_LEVELS = {
        { 130, 520, 1560, 3640, 6240, 9360 },
        { 325, 1300, 3900, 9100, 15600, 23400 },
        { 650, 2600, 7800, 18200, 31200, 46800 },
        { 1300, 5200, 15600, 36400, 62400, 93600 },
        { 2600, 10400, 31200, 72800, 124800, 187200 } };
}
