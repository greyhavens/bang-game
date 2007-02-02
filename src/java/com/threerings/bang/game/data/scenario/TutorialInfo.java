//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Stat;
import com.threerings.bang.game.data.piece.Marker;

/**
 * Contains metadata on the Tutorial scenario.
 */
public class TutorialInfo extends ScenarioInfo
{
    /** The string identifier for this scenario. */
    public static final String IDENT = "tu";

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
    public boolean showDetailedMarquee ()
    {
        return false;
    }

    @Override // from ScenarioInfo
    public boolean isValidMarker (Marker marker)
    {
        return true; // if it's on a tutorial board, we need it
    }

    @Override // from ScenarioInfo
    public Stat.Type[] getObjectives ()
    {
        return null;
    }

    @Override // from ScenarioInfo
    public int[] getPointsPerObjectives ()
    {
        return null;
    }
}
