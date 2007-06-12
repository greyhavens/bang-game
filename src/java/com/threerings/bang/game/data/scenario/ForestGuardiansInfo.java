//
// $Id$

package com.threerings.bang.game.data.scenario;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.StatType;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.client.ForestGuardiansStatsView;
import com.threerings.bang.game.client.StatsView;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.piece.Marker;

/**
 * Contains metadata on the Wendigo Attack scenario.
 */
public class ForestGuardiansInfo extends ScenarioInfo
{
    /** The string identifier for this scenario. */
    public static final String IDENT = "fg";

    /** Points earned at each tick for contributing to trees' growth. */
    public static final int POINTS_PER_TREE_GROWTH = 1;

    /** Stat types for each level of growth (minus one). */
    public static final StatType[] GROWTH_STATS = {
        StatType.TREES_SAPLING, StatType.TREES_MATURE, StatType.TREES_ELDER };

    /** Points awarded for living trees at the end of each wave. */
    public static final int[] GROWTH_POINTS = { 1, 3, 5 };

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
    public int getTeamSize (BangConfig config, int pidx)
    {
        int size = super.getTeamSize(config, pidx);
        if (config.type != BangConfig.Type.BOUNTY) {
            size = Math.min(size, 2);
        }
        return size;
    }

    @Override // from ScenarioInfo
    public StatType[] getObjectives ()
    {
        return GROWTH_STATS;
    }

    @Override // from ScenarioInfo
    public int[] getPointsPerObjectives ()
    {
        return GROWTH_POINTS;
    }

    @Override // from ScenarioInfo
    public String getObjectiveCode ()
    {
        return "trees_grown";
    }

    @Override // from ScenarioInfo
    public StatType getSecondaryObjective ()
    {
        return StatType.TREE_POINTS;
    }

    @Override // from ScenarioInfo
    public boolean isValidMarker (Marker marker)
    {
        return super.isValidMarker(marker) ||
            marker.getType() == Marker.ROBOTS ||
            marker.getType() == Marker.FETISH;
    }

    @Override // from ScenarioInfo
    public int getTeam (int owner, int assignedTeam)
    {
        // all human players are on the same team
        return (owner > -1 ? 0 : assignedTeam);
    }

    @Override // from ScenarioInfo
    public boolean hasEnemies (UnitConfig.Make make)
    {
        return make == UnitConfig.Make.STEAM;
    }

    @Override // from ScenarioInfo
    public Teams getTeams ()
    {
        return Teams.COOP;
    }

    @Override // from ScenarioInfo
    public StatsView getStatsView (BasicContext ctx)
    {
        return new ForestGuardiansStatsView(ctx);
    }
}
