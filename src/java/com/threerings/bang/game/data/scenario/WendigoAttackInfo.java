//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.StatType;
import com.threerings.bang.game.data.piece.Marker;

/**
 * Contains metadata on the Wendigo Attack scenario.
 */
public class WendigoAttackInfo extends ScenarioInfo
{
    /** The string identifier for this scenario. */
    public static final String IDENT = "wa";

    /** Points per unit surviving a wendigo attack. */
    public static final int POINTS_PER_SURVIVAL = 15;

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
    public boolean isValidMarker (Marker marker)
    {
        return super.isValidMarker(marker) || 
            marker.getType() == Marker.SAFE ||
            marker.getType() == Marker.SAFE_ALT ||
            marker.getType() == Marker.TALISMAN;
    }

    @Override // from ScenarioInfo
    public StatType[] getObjectives ()
    {
        return new StatType[] { StatType.WENDIGO_SURVIVALS };
    }

    @Override // from ScenarioInfo
    public int[] getPointsPerObjectives ()
    {
        return new int[] { POINTS_PER_SURVIVAL };
    }

    @Override // from ScenarioInfo
    public StatType getSecondaryObjective ()
    {
        return StatType.TALISMAN_POINTS;
    }
}
