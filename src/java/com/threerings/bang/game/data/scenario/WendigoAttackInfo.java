//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Stat;

/**
 * Contains metadata on the Wendigo Attack scenario.
 */
public class WendigoAttackInfo extends ScenarioInfo
{
    /** The string identifier for this scenario. */
    public static final String IDENT = "wa";

    /** Points per unit surviving a wendigo attack. */
    public static final int POINTS_PER_SURVIVAL = 25;

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
        return Stat.Type.WENDIGO_SURVIVALS;
    }

    @Override // from ScenarioInfo
    public int getPointsPerObjective ()
    {
        return POINTS_PER_SURVIVAL;
    }

    @Override // from ScenarioInfo
    public Stat.Type getSecondaryObjective ()
    {
        return Stat.Type.TALISMAN_POINTS;
    }
}
