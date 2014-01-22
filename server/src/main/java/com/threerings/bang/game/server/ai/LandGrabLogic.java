//
// $Id$

package com.threerings.bang.game.server.ai;

import java.util.List;

import java.awt.Point;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.server.scenario.LandGrab;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Homestead;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Teleporter;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;

/**
 * A simple AI for the land grab scenario.
 */
public class LandGrabLogic extends AILogic
    implements PieceCodes
{
    public LandGrabLogic (LandGrab scenario)
    {
        _scenario = scenario;
    }

    @Override // from AILogic
    public String getBigShotType ()
    {
        // prefer a big shot with greater move distance
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId, UnitConfig.Rank.BIGSHOT);
        return getWeightedUnitTypes(configs, OFFENSE_EVALUATOR, 1)[0];
    }

    @Override // from AILogic
    public String[] getUnitTypes (int count)
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId, UnitConfig.Rank.NORMAL);
        return getWeightedUnitTypes(configs, OFFENSE_EVALUATOR, count);
    }

    @Override // from AILogic
    public void tick (List<Piece> pieces, short tick)
    {
        if (_steads == null) {
            _steads = _scenario.getHomesteads();
        }
        super.tick(pieces, tick);
    }

    @Override // from AILogic
    protected void moveUnit (List<Piece> pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        Point control = getControlCenter(pieces);
        Unit bshot = null; // use special logic for the big shot
        boolean isBShot = (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT);

        if (!isBShot) {
            for (Piece p : pieces) {
                if (p instanceof Unit && p.owner == _pidx && p.isAlive() &&
                    ((Unit)p).getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                    bshot = (Unit)p;
                    continue;
                }
            }
        }

        // figure out which homestead will be targeting
        int ovalue = 0;
        Homestead ostead = null;
        for (Homestead stead : _steads) {
            if (stead.isSameTeam(_bangobj, unit) || (!isBShot && stead.owner < 0)) {
                continue;
            }
            int value = Math.max(20 - unit.getDistance(stead), 0) * 10 +
                stead.damage + Math.max(20 - stead.getDistance(control.x, control.y), 0) * 5;
            if (isBShot) {
                value += (stead.owner == -1 ? 100 : 0);
            } else if (bshot != null) {
                value += Math.max(20 - bshot.getDistance(stead), 0) * 5;
            }
            if (value > ovalue) {
                ostead = stead;
                ovalue = value;
            }
        }

        // find the best thing to shoot in the general direction of the objective homestead
        if (ostead != null) {
            boolean mustMove = false;
            if (isBShot && ostead.owner < 0 && unit.getDistance(ostead.x, ostead.y) == 1) {
                moves.remove(unit.x, unit.y);
                mustMove = true;
            }

            Point mv = getClosestPoint(
                unit, moves, ostead.x, ostead.y, (ostead.owner < 0) ? 1 : -1, mustMove);
            if (mv != null) {
                if (isBShot && ostead.owner < 0 && ostead.getDistance(mv.x, mv.y) == 1) {
                    executeOrder(unit, mv.x, mv.y,
                                 getBestTarget(pieces, unit, mv.x, mv.y, TARGET_EVALUATOR));
                    return;
                }

                PointSet preferred = new PointSet();
                preferred.add(mv.x, mv.y);
                for (int ii = 0; ii < DX.length; ii++) {
                    int x = mv.x + DX[ii], y = mv.y + DY[ii];
                    if (moves.contains(x, y)) {
                        preferred.add(x, y);
                    }
                }

                Piece target = getBestTargetInMoves(
                    pieces, unit, attacks, preferred, mv, TARGET_EVALUATOR);
                executeOrder(unit, mv.x, mv.y, target);
                return;
            }
        }

        // look for a teleporter
        Piece cporter = null;
        for (Teleporter tporter : _bangobj.getTeleporters().values()) {
            if (cporter == null || unit.getDistance(tporter) < unit.getDistance(cporter)) {
                cporter = tporter;
            }
        }
        if (cporter != null &&
            moveUnit(pieces, unit, moves, cporter.x, cporter.y, 0, TARGET_EVALUATOR)) {
            return;
        }

        // otherwise just go shoot at anything
        if (moveUnit(pieces, unit, moves, control.x, control.y, -1, TARGET_EVALUATOR)) {
            return;
        }
        Piece target = getBestTarget(pieces, unit, unit.x, unit.y, TARGET_EVALUATOR);
        if (target != null) {
            executeOrder(unit, Short.MAX_VALUE, 0, target);
        }
    }

    /** Reference to our scenario. */
    protected LandGrab _scenario;

    /** Used to track the locations of all homestead spots. */
    protected List<Homestead> _steads;

    /** Ranks units by properties that should make them good at gathering totems: speed and attack
     * power. */
    protected static final UnitConfigEvaluator OFFENSE_EVALUATOR = new UnitConfigEvaluator() {
        public int getWeight (UnitConfig config) {
            return config.moveDistance*10 + config.damage;
        }
    };

    /** Ranks potential targets by rank, the amount of damage the unit will do, and the amount of
     * damage the target has already taken. */
    protected static final TargetEvaluator TARGET_EVALUATOR = new TargetEvaluator() {
        public int getWeight (BangObject bangobj, Unit unit, Piece target, int dist,
                              PointSet preferredMoves) {
            UnitConfig.Rank rank = (target instanceof Unit) ?
                ((Unit)target).getConfig().rank : null;
            boolean homestead = (target instanceof Homestead);
            return (rank == UnitConfig.Rank.BIGSHOT ? 1000 : 0) +
                unit.computeScaledDamage(bangobj, target, 1f) * 100 +
                (homestead ? target.damage * 5 + 50 : target.damage);
        }
    };
}
