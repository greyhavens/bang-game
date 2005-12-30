//
// $Id$

package com.threerings.bang.game.data;

/**
 * Contains scenario-specific codes and constants.
 */
public interface ScenarioCodes
{
    /** The code for the "overall" scenario (only used for ratings).  */
    public static final String OVERALL = "oa";

    /** The code for the tutorial scenario. */
    public static final String TUTORIAL = "tu";

    /** The code for the claim jumping scenario. */
    public static final String CLAIM_JUMPING = "cj";

    /** The code for the cattle rustling scenario. */
    public static final String CATTLE_RUSTLING = "cr";

    /** The amount of cash earned per nugget at the end of the game. */
    public static final int CASH_PER_NUGGET = 50;

    /** Cash earned for each branded cow. */
    public static final int CASH_PER_COW = 50;
}
