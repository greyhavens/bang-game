//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Stat;

/**
 * Contains metadata on the Practice scenario.
 */
public class PracticeInfo extends ScenarioInfo
{
    /** The string identifier for this scenario. */
    public static final String IDENT = "pr";

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
    public Stat.Type getObjective ()
    {
        return null;
    }

    @Override // from ScenarioInfo
    public int getPointsPerObjective ()
    {
        return 0;
    }
}
