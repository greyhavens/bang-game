//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants related to gangs.
 */
public interface GangCodes extends InvocationCodes
{
    /** Describes one of the "weight classes" for gangs. */
    public static class WeightClass
    {
        /** The cost in aces to upgrade to this class. */
        public int aces;

        /** The coin cost to upgrade. */
        public int coins;

        /** The maximum number of members allowed in this class. */
        public int maxMembers;

        /** The notoriety level cutoffs. */
        public int[] notorietyLevels;

        public WeightClass (int aces, int coins, int maxMembers, int[] notorietyLevels)
        {
            this.aces = aces;
            this.coins = coins;
            this.maxMembers = maxMembers;
            this.notorietyLevels = notorietyLevels;
        }
    }

    /** The message bundle identifier for our translation messages. */
    public static final String GANG_MSGS = "gang";

    /** Error message when a new leader cannot perform a command. */
    public static final String E_NEW_LEADER_WAIT = "e.new_leader_wait";

    /** Error message when a new leader cannot perform a command. */
    public static final String E_MEMBER_LACKS_ONETIME = "e.member_lacks_onetime";

    /** Gang rank constant. */
    public static final byte MEMBER_RANK = 0;

    /** Gang rank constant. */
    public static final byte RECRUITER_RANK = 1;

    /** Gang rank constant. */
    public static final byte LEADER_RANK = 2;

    /** The number of gang ranks. */
    public static final byte RANK_COUNT = 3;

    /** The number of gang member titles. */
    public static final int TITLES_COUNT = 53;

    /** Gang rank string translations. */
    public static final String[] XLATE_RANKS = { "m.member", "m.recruiter", "m.leader" };

    /** The cost of forming a gang in scrip. */
    public static final int FORM_GANG_SCRIP_COST = 2500;

    /** The cost of a forming a gang in coins. */
    public static final int FORM_GANG_COIN_COST = 5;

    /** The percentage of the member's total donations that must be reimbursed if they are kicked
     * out of the gang. */
    public static final int DONATION_REIMBURSEMENT_PCT = 50;

    /** The percentage of the member's total donations that must be reimbursed if they are kicked
     * out of the gang in the initial time period. */
    public static final int EARLY_REIMBURSEMENT_PCT = 100;

    /** The percentage of the member's total donations that must be reimbursed if they are kicked
     * out of the gang after bein inactive. */
    public static final int INACTIVE_REIMBURSEMENT_PCT = 5;

    /** The amount of time that may elapse before gang members are considered to be inactive. */
    public static final long ACTIVITY_DELAY = 14L * 24 * 60 * 60 * 1000;

    /** The amount of time that must elapse before members can contribute to the gang's coffers. */
    public static final long DONATION_DELAY = 7L * 24 * 60 * 60 * 1000;

    /** The amount of time that must elapse before a gang member is considered to be inactive
     * for reimbursement purposes. */
    public static final long DONATION_ACTIVITY_DELAY = 35L * 24 * 60 * 60 * 1000;

    /** The starting number of icons gangs can have on their buckles. */
    public static final int DEFAULT_MAX_BUCKLE_ICONS = 1;

    /** The gang weight classes. */
    public static final WeightClass[] WEIGHT_CLASSES = {
        new WeightClass(0, 0, 20, new int[] { 130, 520, 1560, 3640, 6240, 9360 }),
        new WeightClass(200, 5, 50, new int[] { 325, 1300, 3900, 9100, 15600, 23400 }),
        new WeightClass(400, 10, 100, new int[] { 650, 2600, 7800, 18200, 31200, 46800 }),
        new WeightClass(700, 20, 200, new int[] { 1300, 5200, 15600, 36400, 62400, 93600 }),
        new WeightClass(1000, 40, 500, new int[] { 2600, 10400, 31200, 72800, 124800, 187200 }),
    };
}
