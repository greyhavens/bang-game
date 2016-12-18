//
// $Id$

package com.threerings.bang.game.server.ai;

import java.util.List;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.LoggingRobot;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Teleporter;
import com.threerings.bang.game.data.piece.TreeBed;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;

/**
 * A simple AI for the forest guardians scenario.
 */
public class ForestGuardiansLogic extends AILogic
{
    @Override // documentation inherited
    public String getBigShotType ()
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.BIGSHOT);
        return getWeightedUnitTypes(configs, UNIT_EVALUATOR, 1)[0];
    }

    @Override // documentation inherited
    public String[] getUnitTypes (int count)
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.NORMAL);
        return getWeightedUnitTypes(configs, UNIT_EVALUATOR, count);
    }

    // documentation inherited
    protected void moveUnit (List<Piece> pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        moveUnit(pieces, unit, moves, OBJECTIVE_EVALUATOR, TARGET_EVALUATOR);
    }

    /** Prefers air units (because they're immune to the logging robots'
     * melee attack), higher fire distance, greater damage; avoids using
     * the stormcaller. */
    protected static final UnitConfigEvaluator UNIT_EVALUATOR =
        new UnitConfigEvaluator() {
        public int getWeight (UnitConfig config) {
            if (config.type.equals("indian_post/stormcaller")) {
                return 10;
            }
            return (config.mode == UnitConfig.Mode.AIR ? 100 : 0) +
                config.maxFireDistance*10 + config.damage;
        }
    };

    /** Seeks out trees and logging robots, using teleporters as a last
     * resort. */
    protected static final ObjectiveEvaluator OBJECTIVE_EVALUATOR =
        new ObjectiveEvaluator() {
        public int getWeight (Unit unit, Piece obj) {
            if (obj instanceof TreeBed && obj.isAlive() && obj.damage != 0) {
                TreeBed bed = (TreeBed)obj;
                return 200 + bed.vulnerability - unit.getDistance(bed);
            } else if (obj instanceof LoggingRobot && obj.isAlive()) {
                LoggingRobot bot = (LoggingRobot)obj;
                return 200 + (bot.isSuper() ? 2 : 0) - unit.getDistance(bot);
            } else if (obj instanceof Teleporter) {
                return 100 - unit.getDistance(obj);
            } else {
                return 0;
            }
        }
        public int getDistance (Unit unit, Piece obj) {
            if (obj instanceof TreeBed) {
                return 1;
            } else if (obj instanceof Teleporter) {
                return 0;
            } else {
                return -1;
            }
        }
    };

    /** Prefers stronger logging robots with more damage. */
    protected static final TargetEvaluator TARGET_EVALUATOR =
        new TargetEvaluator() {
        public int getWeight (BangObject bangobj, Unit unit, Piece target,
                int dist, PointSet preferredMoves) {
            return (target instanceof LoggingRobot &&
                ((LoggingRobot)target).isSuper() ? 100 : 0) +
                    (1 + target.damage);
        }
    };
}
