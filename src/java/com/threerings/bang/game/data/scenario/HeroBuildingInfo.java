//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.StatType;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.client.HeroBuildingView;
import com.threerings.bang.game.client.ScenarioHUD;

/**
 * Contains metadata on the Hero Building scenario.
 */
public class HeroBuildingInfo extends ScenarioInfo
{
    /** The string identifier for this scneario. */
    public static final String IDENT = "hb";

    /** The points for each level a hero attains. */
    public static final int POINTS_PER_LEVEL = 50;

    @Override // documentation inherited
    public String getIdent ()
    {
        return IDENT;
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }

    @Override // documentation inherited
    public ScenarioHUD getHUD (BangContext ctx, BangObject bangobj)
    {
        return new HeroBuildingView(ctx, bangobj);
    }

    @Override // documentation inherited
    public String getMusic ()
    {
        return BangCodes.INDIAN_POST + "/scenario_" + TotemBuildingInfo.IDENT;
    }

    @Override // documentation inherited
    public StatType[] getObjectives ()
    {
        return new StatType[] { StatType.HERO_LEVEL };
    }

    @Override // documentation inherited
    public int[] getPointsPerObjectives ()
    {
        return new int[] { POINTS_PER_LEVEL };
    }
}
