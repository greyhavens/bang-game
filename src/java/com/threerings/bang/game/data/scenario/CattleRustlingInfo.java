//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.StatType;
import com.threerings.bang.game.data.piece.Marker;

/**
 * Contains metadata on the Cattle Rustling scenario.
 */
public class CattleRustlingInfo extends ScenarioInfo
{
    /** The string identifier for this scenario. */
    public static final String IDENT = "cr";

    /** Points earned for each branded cow. */
    public static final int POINTS_PER_COW = 50;

    /** Points earned at each tick per branded cow. */
    public static final int POINTS_PER_BRAND = 1;

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
    public StatType[] getObjectives ()
    {
        return new StatType[] { StatType.CATTLE_RUSTLED };
    }

    @Override // from ScenarioInfo
    public int[] getPointsPerObjectives ()
    {
        return new int[] { POINTS_PER_COW };
    }

    @Override // from ScenarioInfo
    public StatType getSecondaryObjective ()
    {
        return StatType.BRAND_POINTS;
    }

    @Override // from ScenarioInfo
    public boolean isValidMarker (Marker marker)
    {
        return super.isValidMarker(marker) || marker.getType() == Marker.CATTLE;
    }
    
    @Override // from ScenarioInfo
    public boolean hasHoldableBonuses ()
    {
        return false;
    }
}
