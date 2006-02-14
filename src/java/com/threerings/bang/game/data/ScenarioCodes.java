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

    /** The amount of points earned per nugget at the end of the game. */
    public static final int POINTS_PER_NUGGET = 50;

    /** The code for the cattle rustling scenario. */
    public static final String CATTLE_RUSTLING = "cr";

    /** Points earned for each branded cow. */
    public static final int POINTS_PER_COW = 50;

    /** Scenarios available in Frontier Town. */
    public static final String[] FRONTIER_TOWN_SCENARIOS = {
        CLAIM_JUMPING, CATTLE_RUSTLING };

    /** Scenarios available in Indian Village. */
    public static final String[] INDIAN_VILLAGE_SCENARIOS = { };

    /** Scenarios available in Boom Town. */
    public static final String[] BOOM_TOWN_SCENARIOS = { };

    /** Scenarios available in Ghost Town. */
    public static final String[] GHOST_TOWN_SCENARIOS = { };

    /** Scenarios available in the City of Gold. */
    public static final String[] CITY_OF_GOLD_SCENARIOS = { };
}
