//
// $Id$

package com.threerings.bang.game.server.ai;

import java.awt.Point;
import java.util.List;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Teleporter;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.*;

/**
 * A simple AI for the gold-based scenarios: claim jumping and gold rush.
 */
public class GoldLogic extends AILogic
    implements PieceCodes
{
    /**
     * Creates the AI logic for a gold-based scenario.
     *
     * @param stealing whether or not units can steal nuggets from claims (i.e., whether this is
     * for the claim jumping scenario)
     */
    public GoldLogic (boolean stealing)
    {
        _stealing = stealing;
    }

    // documentation inherited
    public String getBigShotType ()
    {
        // prefer a big shot with greater move distance
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId, UnitConfig.Rank.BIGSHOT);
        return getWeightedUnitTypes(configs, OFFENSE_EVALUATOR, 1)[0];
    }

    // documentation inherited
    public String[] getUnitTypes (int count)
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId, UnitConfig.Rank.NORMAL);
        return getWeightedUnitTypes(configs, OFFENSE_EVALUATOR, count);
    }

    // documentation inherited
    protected void moveUnit (List<Piece> pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        // search for own claim, closest enemy claim with nuggets, closest enemy with nugget,
        // closest free nugget, and enemies near our claim
        Counter oclaim = null, cclaim = null;
        Unit ctarget = null;
        Piece cnugget = null, tporter = null;
        boolean breached = false;
        for (Piece p : pieces) {
            if (p instanceof Counter) {
                Counter claim = (Counter)p;
                if (claim.owner == _pidx) {
                    oclaim = claim;
                } else if (_stealing && claim.count > 0 &&
                           claim.getTeam(_bangobj) != _bangobj.getTeam(_pidx) &&
                           (cclaim == null || unit.getDistance(claim) < unit.getDistance(cclaim))) {
                    cclaim = claim;
                }

            } else if (NuggetEffect.isNuggetBonus(p)) {
                if (cnugget == null || unit.getDistance(p) < unit.getDistance(cnugget)) {
                    cnugget = p;
                }

            } else if (p instanceof Unit && p.owner != _pidx) {
                Unit target = (Unit)p;
                if (NuggetEffect.isNuggetBonus(target.holding) &&
                    (ctarget == null ||
                     unit.getDistance(target) < unit.getDistance(ctarget)) &&
                    unit.validTarget(_bangobj, target, false)) {
                    ctarget = target;
                }
                if (_stealing && _claimloc != null &&
                    target.getDistance(_claimloc.x, _claimloc.y) <= DEFENSIVE_PERIMETER) {
                    breached = true;
                }

            } else if (p instanceof Teleporter && (tporter == null ||
                       unit.getDistance(p) < unit.getDistance(tporter))) {
                tporter = p;
            }
        }

        if (oclaim == null) {
            log.warning("Missing own counter for AI unit", "where", _bangmgr.where(),
                        "pidx", _pidx);
            return;
        }
        if (_claimloc == null) {
            _claimloc = new Point(oclaim.x, oclaim.y);
        }

        // if we have a nugget or our claim is in danger, haul ass back home
        if ((NuggetEffect.isNuggetBonus(unit.holding) ||
             (breached && oclaim.count > 0 && unit.getDistance(oclaim) > DEFENSIVE_PERIMETER)) &&
            moveUnit(pieces, unit, moves, oclaim, 1)) {

        // if there's a nugget within reach, grab it
        } else if (cnugget != null && moves.contains(cnugget.x, cnugget.y)) {
            executeOrder(unit, cnugget.x, cnugget.y,
                         getBestTarget(pieces, unit, cnugget.x, cnugget.y, TARGET_EVALUATOR));

        // if there's a loaded claim within reach, steal from it
        } else if (cclaim != null && containsAdjacent(moves, cclaim) &&
                   moveUnit(pieces, unit, moves, cclaim, 1)) {
            // moveUnit did what we wanted

        // if there's a nugget holding target within reach, shoot it
        } else if (ctarget != null && attacks.contains(ctarget.x, ctarget.y)) {
            executeOrder(unit, Short.MAX_VALUE, 0, ctarget);

        // otherwise, move towards nearest free nugget
        } else if (cnugget != null &&
            moveUnit(pieces, unit, moves, cnugget, 0)) {

        // or nearest loaded claim
        } else if (cclaim != null && moveUnit(pieces, unit, moves, cclaim, 1)) {
            // moveUnit did what we wanted

        // or nearest nugget holding target
        } else if (ctarget != null && moveUnit(pieces, unit, moves, ctarget, -1)) {
            // moveUnit did what we wanted

        // or nearest teleporter
        } else if (tporter != null && moveUnit(pieces, unit, moves, tporter, 0)) {
            // moveUnit did what we wanted

        // or just try to find something to shoot
        } else {
            Piece target = getBestTarget(pieces, unit, attacks, EMPTY_POINT_SET, TARGET_EVALUATOR);
            if (target != null) {
                executeOrder(unit, Short.MAX_VALUE, 0, target);
            }
        }
    }

    /**
     * Attempts to move the unit towards the provided destination and fire off a shot at the best
     * target.
     *
     * @return true if we successfully moved towards the destination, false if we couldn't find a
     * path
     */
    protected boolean moveUnit (List<Piece> pieces, Unit unit, PointSet moves, Piece target,
                                int tdist)
    {
        return moveUnit(pieces, unit, moves, target.x, target.y, tdist, TARGET_EVALUATOR);
    }

    /**
     * Determines whether the point set contains any points adjacent to the given piece.
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

    /** Whether or not units can steal nuggets from claims. */
    protected boolean _stealing;

    /** The location of our own claim. */
    protected Point _claimloc;

    /** Ranks units by properties that should make them good at gathering and stealing nuggets:
     * speed and attack power. */
    protected static final UnitConfigEvaluator OFFENSE_EVALUATOR =
        new UnitConfigEvaluator() {
        public int getWeight (UnitConfig config) {
            return config.moveDistance*10 + config.damage;
        }
    };

    /** Ranks potential targets by nugget holdingness, the amount of damage the unit will do, and
     * the amount of damage the target has already taken. */
    protected static final TargetEvaluator TARGET_EVALUATOR = new TargetEvaluator() {
        public int getWeight (BangObject bangobj, Unit unit, Piece target, int dist,
                              PointSet preferredMoves) {
            boolean nuggeted = (target instanceof Unit &&
                                NuggetEffect.isNuggetBonus(((Unit)target).holding));
            return (nuggeted ?  1000 : 0) + unit.computeScaledDamage(bangobj, target, 1f) * 100 +
                target.damage;
        }
    };

    /** When enemy units get this close to our (non-empty) claim, we start
     * sending units to defend it. */
    protected static final int DEFENSIVE_PERIMETER = 3;
}
