//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Stat;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.piece.Marker;

/**
 * Contains metadata on the Wendigo Attack scenario.
 */
public class ForestGuardiansInfo extends ScenarioInfo
{
    /** The string identifier for this scenario. */
    public static final String IDENT = "fg";

    /** Points per tree grown. */
    public static final int POINTS_PER_TREE = 25;

    /** Points earned at each tick for contributing to trees' growth. */
    public static final int POINTS_PER_TREE_GROWTH = 1;
    
    @Override // from ScenarioInfo
    public String getIdent ()
    {
        return IDENT;
    }

    @Override // from ScenarioInfo
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }

    @Override // from ScenarioInfo
    public int getTeamSize (BangConfig config)
    {
        return Math.min(config.teamSize, 2);
    }
    
    @Override // from ScenarioInfo
    public Stat.Type getObjective ()
    {
        return Stat.Type.TREES_GROWN;
    }

    @Override // from ScenarioInfo
    public int getPointsPerObjective ()
    {
        return POINTS_PER_TREE;
    }
    
    @Override // from ScenarioInfo
    public Stat.Type getSecondaryObjective ()
    {
        return Stat.Type.TREE_POINTS;
    }
    
    @Override // from ScenarioInfo
    public boolean isValidMarker (Marker marker)
    {
        return super.isValidMarker(marker) || marker.getType() == Marker.ROBOTS;
    }
    
    @Override // from ScenarioInfo
    public boolean playersAllied (int pidx1, int pidx2)
    {
        return (pidx1 < 0) == (pidx2 < 0);
    }
}
