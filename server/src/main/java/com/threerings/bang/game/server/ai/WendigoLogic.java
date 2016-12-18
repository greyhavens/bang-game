//
// $Id$

package com.threerings.bang.game.server.ai;

import java.awt.Point;
import java.util.List;

import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.server.scenario.WendigoAttack;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.TalismanEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Teleporter;
import com.threerings.bang.game.data.piece.ToggleSwitch;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;

/**
 * A simple AI for the wendigo attack scenario.
 */
public class WendigoLogic extends AILogic
    implements PieceCodes
{
    public WendigoLogic (WendigoAttack scenario)
    {
        _scenario = scenario;
    }

    // documentation inherited
    public String getBigShotType ()
    {
        // prefer a big shot with greater move distance
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.BIGSHOT);
        return getWeightedUnitTypes(configs, OFFENSE_EVALUATOR, 1)[0];
    }

    // documentation inherited
    public String[] getUnitTypes (int count)
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.NORMAL);
        return getWeightedUnitTypes(configs, OFFENSE_EVALUATOR, count);
    }

    @Override // documentation inherited
    public void tick (List<Piece> pieces, short tick)
    {
        super.tick(pieces, tick);
    }

    // documentation inherited
    protected void moveUnit (List<Piece> pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        PointSet safeSpots = _scenario.getSafeSpots();
        Unit ctarget = null;
        Piece talisman = null, tporter = null;
        ToggleSwitch tswitch = null;
        int safeties = 0;
        for (Piece p : pieces) {
            if (Bonus.isBonus(p, TalismanEffect.TALISMAN_BONUS)) {
                if (talisman == null || unit.getDistance(p) <
                        unit.getDistance(talisman)) {
                    talisman = p;
                }

            } else if (p instanceof ToggleSwitch &&
                    ((ToggleSwitch)p).isActive(_bangobj.tick)) {
                if (tswitch == null ||
                        unit.getDistance(p) < unit.getDistance(tswitch)) {
                    tswitch = (ToggleSwitch)p;
                }

            } else if (p instanceof Unit && p.owner != _pidx) {
                Unit target = (Unit)p;
                if (TalismanEffect.TALISMAN_BONUS.equals(target.holding) &&
                    (ctarget == null ||
                     unit.getDistance(target) < unit.getDistance(ctarget)) &&
                    unit.validTarget(_bangobj, target, false)) {
                    ctarget = target;
                }

            } else if (p instanceof Unit && safeSpots.contains(p.x, p.y)) {
                safeties++;

            } else if (p instanceof Teleporter && (tporter == null ||
                unit.getDistance(p) < unit.getDistance(tporter))) {
                tporter = p;
            }
        }
        PointSet preferredMoves = (PointSet)moves.clone();
        preferredMoves.retainAll(safeSpots);
        boolean inDanger = _scenario.areWendigoPrepared();
        boolean holdingTalisman = TalismanEffect.TALISMAN_BONUS.equals(
                unit.holding);

        // if in danger try to run to safety
        if (inDanger && !holdingTalisman && !preferredMoves.isEmpty()) {
            // find someone to shoot while moving to safety
            Point move = new Point();
            Piece target = getBestTargetInMoves(pieces, unit, attacks,
                    preferredMoves, move, TARGET_EVALUATOR);
            if (target != null) {
                executeOrder(unit, move.x, move.y, target);

            // if we can't find someone to shoot, just move to safety
            } else {
                int midx = RandomUtil.getInt(preferredMoves.size());
                executeOrder(unit, preferredMoves.getX(midx),
                        preferredMoves.getY(midx), null);
            }
            return;

        // if there's a talisman within reach, grab it
        } else if (!holdingTalisman && talisman != null &&
                    moves.contains(talisman.x, talisman.y)) {
            executeOrder(unit, talisman.x, talisman.y, getBestTarget(
                pieces, unit, talisman.x, talisman.y, TARGET_EVALUATOR));
            return;

        // if there's a talisman holding target within reach, shoot it
        } else if (ctarget != null && attacks.contains(ctarget.x, ctarget.y)) {
            Point mv = unit.computeShotLocation(_bangobj.board, ctarget,
                    moves, false, preferredMoves);
            if (mv != null) {
                executeOrder(unit, (short)mv.x, (short)mv.y, ctarget);
            } else {
                executeOrder(unit, Short.MAX_VALUE, 0, ctarget);
            }
            return;

        } else if (tswitch != null && safeties == 0 &&
                moves.contains(tswitch.x, tswitch.y)) {
            executeOrder(unit, tswitch.x, tswitch.y, getBestTarget(
                pieces, unit, tswitch.x, tswitch.y, TARGET_EVALUATOR));
            return;
        }

        int dist = Integer.MAX_VALUE;
        Point safe = new Point(unit.x, unit.y);
        for (int ii = 0; ii < safeSpots.size(); ii++) {
            int x = safeSpots.getX(ii), y = safeSpots.getY(ii);
            int tdist = unit.getDistance(x, y);
            if (tdist < dist && _bangobj.board.isOccupiable(x, y)) {
                dist = tdist;
                safe.setLocation(x, y);
            }
        }
        // if we're closer to a safe zone, move there
        if ((talisman == null || dist < unit.getDistance(talisman)) &&
                (ctarget == null || dist < unit.getDistance(ctarget)) &&
                 moveUnit(pieces, unit, moves, safe.x, safe.y, 0, TARGET_EVALUATOR)) {
            return;

        // otherwise, move towards nearest free talisman
        } else if (!holdingTalisman && moveUnit(pieces, unit, moves, talisman, 0)) {
            return;

        // or nearest talisman holding target
        } else if (moveUnit(pieces, unit, moves, ctarget, -1)) {
            return;

        // or nearest teleporter
        } else if (moveUnit(pieces, unit, moves, tporter, 0)) {
            return;

        // or nearest switch
        } else if (moveUnit(pieces, unit, moves, tswitch, 0)) {
            return;

        } else {
            // shoot anyone we can find
            Piece target = getBestTarget(pieces, unit, attacks, preferredMoves, TARGET_EVALUATOR);
            if (target != null) {
                executeOrder(unit, Short.MAX_VALUE, 0, target);
            }
        }
    }

    /**
     * Attempts to move the unit towards the provided destination and fire
     * off a shot at the best target.
     *
     * @return true if we successfully moved towards the destination,
     * false if we couldn't find a path
     */
    protected boolean moveUnit (List<Piece> pieces, Unit unit, PointSet moves, Piece target,
                                int tdist)
    {
        return (target != null) && moveUnit(pieces, unit, moves, target.x,
            target.y, tdist, TARGET_EVALUATOR);
    }

    /** Ranks units by properties that should make them good at getting to safe
     * zones: speed and attack power. */
    protected static final UnitConfigEvaluator OFFENSE_EVALUATOR =
        new UnitConfigEvaluator() {
        public int getWeight (UnitConfig config) {
            return config.moveDistance*10 + config.damage;
        }
    };

    /** Ranks potential targets by talsman holdingness, inside safe area,
     * the amount of damage the unit will do, and the amount of damage the
     * target has already taken. */
    protected static final TargetEvaluator TARGET_EVALUATOR =
        new TargetEvaluator() {

        public int getWeight (BangObject bangobj, Unit unit, Piece target,
                int dist, PointSet preferredMoves) {
            int preferredBonus = (preferredMoves.isEmpty() ||
                    unit.computeShotLocation(bangobj.board, target,
                        preferredMoves, true) == null) ? 0 : 5000;
            return ((target instanceof Unit) &&
                    TalismanEffect.TALISMAN_BONUS.equals(
                        ((Unit)target).holding) ? 1000 : 0) +
                preferredBonus +
                unit.computeScaledDamage(bangobj, target, 1f) *
                100 + target.damage;
        }
    };

    /** Reference to the scenario. */
    protected WendigoAttack _scenario;
}
