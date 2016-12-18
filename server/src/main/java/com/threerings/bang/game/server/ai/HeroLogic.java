//
// $Id$

package com.threerings.bang.game.server.ai;

import java.util.List;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Teleporter;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.server.scenario.HeroDelegate;
import com.threerings.bang.game.util.PointSet;

/**
 * A simple AI for the Hero Building scenario.
 */
public class HeroLogic extends AILogic
    implements PieceCodes
{

    public HeroLogic (HeroDelegate herodel)
    {
        _herodel = herodel;
    }

    // documentation inherited
    public String getBigShotType ()
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId, UnitConfig.Rank.BIGSHOT);
        return getWeightedUnitTypes(configs, OFFENSE_EVALUATOR, 1)[0];
    }

    // documentation inherited
    public String[] getUnitTypes (int count)
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId, UnitConfig.Rank.NORMAL);
        return getWeightedUnitTypes(configs, OFFENSE_EVALUATOR, count);
    }

    @Override // documentation inherited
    protected void moveUnit (List<Piece> pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        Unit hero = null, ehero = null;
        PointSet bonuses = new PointSet();
        Piece tporter = null, bonus = null;
        boolean canKill = false;

        for (Piece p : pieces) {
            if (p instanceof Unit) {
                Unit u = (Unit)p;
                if (u.owner == unit.owner && u.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                    hero = u;
                } else if (u.owner != unit.owner && u.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                    if (ehero == null ||
                            unit.getDistance(ehero.x, ehero.y) > unit.getDistance(u.x, u.y)) {
                        ehero = u;
                    }
                }

                if (u.owner != unit.owner && attacks.contains(u.x, u.y) &&
                        unit.killShot(_bangobj, u)) {
                    canKill = true;
                }

            } else if (p instanceof Bonus && ((Bonus)p).isScenarioBonus()) {
                if (moves.contains(p.x, p.y)) {
                    bonuses.add(p.x, p.y);
                    if (bonus == null || unit.getDistance(p.x, p.y) <
                            unit.getDistance(bonus.x, bonus.y)) {
                        bonus = p;
                    }
                }

            } else if (p instanceof Teleporter && (tporter == null ||
                       unit.getDistance(p) < unit.getDistance(tporter))) {
                tporter = p;
            }
        }

        // let the hero kill if they can
        if (unit == hero && canKill) {
            Piece target = getBestTarget(pieces, unit, attacks, bonuses, KILL_EVALUATOR);
            if (target != null) {
                executeOrder(unit, Short.MAX_VALUE, 0, target);
                return;
            }
        }
        // if the hero's hurt, try to heal them
        if (hero != null && hero.damage > 50 && bonus != null &&
                moveUnit(pieces, unit, moves, bonus.x, bonus.y, 0, TARGET_EVALUATOR)) {
            return;

        // then kill if you can
        } else if (canKill) {
            Piece target = getBestTarget(pieces, unit, attacks, bonuses, KILL_EVALUATOR);
            if (target != null) {
                executeOrder(unit, Short.MAX_VALUE, 0, target);
                return;
            }
        }

        // otherwise try to attack someone
        Piece target = getBestTarget(pieces, unit, attacks, bonuses, TARGET_EVALUATOR);
        if (target != null) {
            executeOrder(unit, Short.MAX_VALUE, 0, target);
            return;
        }

        // or head off to the nearest enemy hero
        if (ehero != null && moveUnit(pieces, unit, moves, ehero.x, ehero.y,
                    -unit.getConfig().minFireDistance, TARGET_EVALUATOR)) {
            return;

        // or head to the nearest teleporter
        } else if (tporter != null &&
                moveUnit(pieces, unit, moves, tporter.x, tporter.y, 0, TARGET_EVALUATOR)) {
            return;
        }
    }

    /** Ranks units by properties that should make them good for hero building. */
    protected static final UnitConfigEvaluator OFFENSE_EVALUATOR =
        new UnitConfigEvaluator () {
            public int getWeight (UnitConfig config) {
                return config.moveDistance + config.maxFireDistance;
            }
        };

    /** Ranks potential targets by benefits for killing. */
    protected TargetEvaluator KILL_EVALUATOR =
        new TargetEvaluator() {
            public int getWeight (BangObject bangobj, Unit unit, Piece target, int dist,
                    PointSet preferredMoves) {
                if (!unit.killShot(bangobj, target)) {
                    return 0;
                }
                int weight = (preferredMoves.isEmpty() ||
                        unit.computeShotLocation(
                            bangobj.board, target, preferredMoves, true) == null) ? 0 : 1000;
                if (target instanceof Unit &&
                        ((Unit)target).getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                    weight += 5000;
                }
                if (target.owner >= 0) {
                    weight += _herodel.getLevel(target.owner) * 500;
                }
                return weight;
            }
        };

    /** Ranks potential targets by benefits for non-killing. */
    protected static final TargetEvaluator TARGET_EVALUATOR =
        new TargetEvaluator() {
            public int getWeight (BangObject bangobj, Unit unit, Piece target, int dist,
                    PointSet preferredMoves) {
                int weight = (target instanceof Unit &&
                        ((Unit)target).getConfig().rank == UnitConfig.Rank.BIGSHOT) ? 500 : 0;
                return weight + unit.computeScaledDamage(bangobj, target, 1f) * 10 + target.damage;
            }
        };

    protected HeroDelegate _herodel;
}
