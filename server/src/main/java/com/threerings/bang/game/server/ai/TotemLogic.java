//
// $Id$

package com.threerings.bang.game.server.ai;

import java.util.ArrayList;
import java.util.List;

import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Teleporter;
import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.TotemBonus;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PointSet;

/**
 * A simple AI for the totem building scenario.
 */
public class TotemLogic extends AILogic
    implements PieceCodes
{
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

    // documentation inherited
    protected void moveUnit (List<Piece> pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        TotemBase cbase = null;
        ArrayList<TotemBase> baseAttack = new ArrayList<TotemBase>(),
            baseMove = new ArrayList<TotemBase>();
        Unit ctarget = null;
        Piece ctotem = null, tporter = null;
        for (Piece p : pieces) {
            if (p instanceof TotemBase) {
                TotemBase base = (TotemBase)p;
                if (!base.canAddPiece()) {
                    continue;
                }
                if (cbase == null ||
                        unit.getDistance(base) < unit.getDistance(cbase)) {
                    cbase = base;
                }
                if (base.numPieces() > 0 && base.getTopOwner() != _pidx) {
                    if (moves.contains(base.x, base.y)) {
                        baseMove.add(base);
                    }
                    if (attacks.contains(base.x, base.y)) {
                        baseAttack.add(base);
                    }
                }
            } else if (p instanceof TotemBonus) {
                if (ctotem == null || unit.getDistance(p) <
                        unit.getDistance(ctotem)) {
                    ctotem = p;
                }
            } else if (p instanceof Unit &&
                    p.owner != _pidx) {
                Unit target = (Unit)p;
                if (TotemBonus.isHolding(target) &&
                    (ctarget == null ||
                     unit.getDistance(target) < unit.getDistance(ctarget)) &&
                    unit.validTarget(_bangobj, target, false)) {
                    ctarget = target;
                }
            } else if (p instanceof Teleporter && (tporter == null ||
                unit.getDistance(p) < unit.getDistance(tporter))) {
                tporter = p;
            }
        }

        // if we're holding a totem piece, let's try to place it
        if (TotemBonus.isHolding(unit) && moveUnit(pieces, unit, moves, cbase, 1)) {
            return;
        }

        // if there's a totem within reach, grab it
        if (ctotem != null && moves.contains(ctotem.x, ctotem.y)) {
            executeOrder(unit, ctotem.x, ctotem.y, getBestTarget(
                pieces, unit, ctotem.x, ctotem.y, TARGET_EVALUATOR));
            return;
        }

        // if there's a totem holding target within reach, shoot it
        if (ctarget != null && attacks.contains(ctarget.x, ctarget.y)) {
            executeOrder(unit, Short.MAX_VALUE, 0, ctarget);
            return;
        }

        // if there's a loaded base within reach, shoot it
        if (!baseAttack.isEmpty() && executeOrder(
                unit, Short.MAX_VALUE, 0, baseAttack.get(RandomUtil.getInt(baseAttack.size())))) {
            return;
        }

        // otherwise, move towards nearest free totem
        if (ctotem != null && moveUnit(pieces, unit, moves, ctotem, 0)) {
            return;
        }

        // or nearest loaded base
        // if there's a loaded base within reach, shoot it
        if (!baseMove.isEmpty() &&
            moveUnit(pieces, unit, moves, baseMove.get(RandomUtil.getInt(baseAttack.size())), -1)) {
            return;
        }

        // or nearest totem holding target
        if (ctarget != null && moveUnit(pieces, unit, moves, ctarget, -1)) {
            return;
        }

        // or nearest teleporter
        if (tporter != null && moveUnit(pieces, unit, moves, tporter, 0)) {
            return;
        }

        // or just try to find something to shoot
        Piece target = getBestTarget(pieces, unit, attacks, EMPTY_POINT_SET, TARGET_EVALUATOR);
        if (target != null) {
            executeOrder(unit, Short.MAX_VALUE, 0, target);
        }
    }

    /**
     * Attempts to move the unit towards the provided destination and fire
     * off a shot at the best target.
     *
     * @return true if we successfully moved towards the destination,
     * false if we couldn't find a path
     */
    protected boolean moveUnit (
        List<Piece> pieces, Unit unit, PointSet moves, Piece target, int tdist)
    {
        return (target != null) && moveUnit(pieces, unit, moves, target.x,
            target.y, tdist, TARGET_EVALUATOR);
    }

    /**
     * Determines whether the point set contains any points adjacent to the
     * given piece.
     */
    protected static boolean containsAdjacent (PointSet moves, Piece piece)
    {
        for (int ii = 0; ii < DIRECTIONS.length; ii++) {
            if (moves.contains(piece.x + DX[ii], piece.y + DY[ii])) {
                return true;
            }
        }
        return false;
    }

    /** Ranks units by properties that should make them good at gathering
     * totems: speed and attack power. */
    protected static final UnitConfigEvaluator OFFENSE_EVALUATOR =
        new UnitConfigEvaluator() {
        public int getWeight (UnitConfig config) {
            return config.moveDistance*10 + config.damage;
        }
    };

    /** Ranks potential targets by totem holdingness, the amount of damage the
     * unit will do, and the amount of damage the target has already taken. */
    protected static final TargetEvaluator TARGET_EVALUATOR =
        new TargetEvaluator() {

        public int getWeight (BangObject bangobj, Unit unit, Piece target,
                int dist, PointSet preferredMoves) {
            // don't go an shoot the totem we just put our piece on
            if ((target instanceof TotemBase) && TotemBonus.isHolding(unit) &&
                    dist == 1) {
                return -1;
            }
            return ((target instanceof Unit) &&
                    TotemBonus.isHolding((Unit)target) ? 1000 : 0) +
                unit.computeScaledDamage(bangobj, target, 1f) *
                100 + target.damage;
        }
    };

    /** When enemy units get this close to our totem base, we start
     * sending units to defend it. */
    protected static final int DEFENSIVE_PERIMETER = 3;

}
