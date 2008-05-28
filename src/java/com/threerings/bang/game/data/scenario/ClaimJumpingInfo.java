//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.piece.Marker;

/**
 * Contains metadata on the Claim Jumping scenario.
 */
public class ClaimJumpingInfo extends NuggetScenarioInfo
{
    /** The string identifier for this scenario. */
    public static final String IDENT = "cj";

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
    public boolean isValidMarker (Marker marker)
    {
        return super.isValidMarker(marker) || marker.getType() == Marker.LODE;
    }
}
