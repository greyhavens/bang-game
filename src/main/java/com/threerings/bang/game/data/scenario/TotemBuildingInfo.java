//
// $Id$

package com.threerings.bang.game.data.scenario;

 
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.StatType;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.client.StatsView;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.TotemBonus;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.util.PointSet;

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
    public StatType[] getObjectives ()
    {
        return new StatType[] {
            StatType.TOTEMS_SMALL, StatType.TOTEMS_MEDIUM,
            StatType.TOTEMS_LARGE, StatType.TOTEMS_CROWN
        };
    }

    @Override // from ScenarioInfo
    public int[] getPointsPerObjectives ()
    {
        return new int[] {
            TotemBonus.Type.TOTEM_SMALL.value(),
            TotemBonus.Type.TOTEM_MEDIUM.value(),
            TotemBonus.Type.TOTEM_LARGE.value(),
            TotemBonus.Type.TOTEM_CROWN.value(),
        };
    }

    @Override // from ScenarioInfo
    public String getObjectiveCode ()
    {
        return "totems_stacked";
    }
    
    @Override // from ScenarioInfo
    public StatType getSecondaryObjective ()
    {
        return StatType.TOTEM_POINTS;
    }

    @Override // from ScenarioInfo
    public boolean isValidMarker (Marker marker)
    {
        return super.isValidMarker(marker) || marker.getType() == Marker.TOTEM;
    }

    @Override // from ScenarioInfo
    public boolean validShot (Unit shooter, PointSet moves, Piece target)
    {
        if (!shooter.shootsFirst() && TotemBonus.isHolding(shooter) && 
                target instanceof TotemBase) { 
            for (int ii = 0, nn = moves.size(); ii < nn; ii++) {
                if (target.getDistance(moves.getX(ii), moves.getY(ii)) != 1) {
                    return super.validShot(shooter, moves, target);
                }
            }
            return false;
        }
        return super.validShot(shooter, moves, target);
    }

    @Override // from ScenarioInfo
    public StatsView getStatsView (BasicContext ctx)
    {
        return new StatsView(ctx, true);
    }
}
