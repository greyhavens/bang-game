//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Stat;
import com.threerings.bang.game.data.piece.Marker;

/**
 * Contains metadata on the Totem Building scenario.
 */
public class TotemBuildingInfo extends ScenarioInfo
{
    /** The string identifier for this scenario. */
    public static final String IDENT = "tb";

    /** Points earned for each totem piece. */
    public static final int POINTS_PER_TOTEM = 25;

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
    public Stat.Type getObjective ()
    {
        return Stat.Type.TOTEMS_STACKED;
    }

    @Override // from ScenarioInfo
    public int getPointsPerObjective ()
    {
        return POINTS_PER_TOTEM;
    }

    @Override // from ScenarioInfo
    public Stat.Type getSecondaryObjective ()
    {
        return Stat.Type.TOTEM_POINTS;
    }

    @Override // from ScenarioInfo
    public boolean isValidMarker (Marker marker)
    {
        return super.isValidMarker(marker) || marker.getType() == Marker.TOTEM;
    }
}
