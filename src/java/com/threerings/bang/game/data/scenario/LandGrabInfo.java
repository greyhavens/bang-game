//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Stat;

/**
 * Contains metadata on the Land Grab scenario.
 */
public class LandGrabInfo extends ScenarioInfo
{
    /** The string identifier for this scenario. */
    public static final String IDENT = "lg";

    /** The points earned per claimed homestead at the end of the game. */
    public static final int POINTS_PER_STEAD = 50;

    /** Points earned at each tick per owned homestead. */
    public static final int POINTS_PER_STEAD_TICK = 1;
    
    @Override // from ScenarioInfo
    public String getIdent ()
    {
        return IDENT;
    }

    @Override // from ScenarioInfo
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // from ScenarioInfo
    public String getMusic ()
    {
        return getTownId() + "/scenario_" + GoldRushInfo.IDENT;
    }

    @Override // from ScenarioInfo
    public Stat.Type[] getObjectives ()
    {
        return new Stat.Type[] { Stat.Type.STEADS_CLAIMED };
    }

    @Override // from ScenarioInfo
    public int[] getPointsPerObjectives ()
    {
        return new int[] { POINTS_PER_STEAD };
    }

    @Override // from ScenarioInfo
    public Stat.Type getSecondaryObjective ()
    {
        return Stat.Type.STEAD_POINTS;
    }
}
