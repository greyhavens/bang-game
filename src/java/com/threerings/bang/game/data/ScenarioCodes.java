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

    /** The code for the practice scenario. */
    public static final String PRACTICE = "pr";

    /** The code for the claim jumping scenario. */
    public static final String CLAIM_JUMPING = "cj";

    /** The code for the gold rush scenario. */
    public static final String GOLD_RUSH = "gr";

    /** The amount of points earned per nugget at the end of the game. */
    public static final int POINTS_PER_NUGGET = 50;

    /** The code for the cattle rustling scenario. */
    public static final String CATTLE_RUSTLING = "cr";

    /** Points earned for each branded cow. */
    public static final int POINTS_PER_COW = 50;

    /** Points earned at each tick per branded cow. */
    public static final int POINTS_PER_BRAND = 1;

    /** Scenarios available in Frontier Town. */
    public static final String[] FRONTIER_TOWN_SCENARIOS = {
        CLAIM_JUMPING, CATTLE_RUSTLING, GOLD_RUSH };

    /** The code for the totem building scenario. */
    public static final String TOTEM_BUILDING = "tb";

    /** Points earned for each totem piece. */
    public static final int POINTS_PER_TOTEM = 25;

    /** The code for the wendigo attack scenario. */
    public static final String WENDIGO_ATTACK = "wa";

    /** Points per unit surviving a wendigo attack. */
    public static final int POINTS_PER_SURVIVAL = 5;

    /** Scenarios available in Indian Trading Post. */
    public static final String[] INDIAN_POST_SCENARIOS = { 
        TOTEM_BUILDING, WENDIGO_ATTACK };

    /** Scenarios available in Boom Town. */
    public static final String[] BOOM_TOWN_SCENARIOS = { };

    /** Scenarios available in Ghost Town. */
    public static final String[] GHOST_TOWN_SCENARIOS = { };

    /** Scenarios available in the City of Gold. */
    public static final String[] CITY_OF_GOLD_SCENARIOS = { };
}
