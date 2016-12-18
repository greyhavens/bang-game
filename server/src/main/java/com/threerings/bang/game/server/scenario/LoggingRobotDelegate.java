//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.List;
import java.util.Iterator;

import com.google.common.collect.Lists;

import com.jme.math.FastMath;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.StatType;

import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.piece.LoggingRobot;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Teleporter;
import com.threerings.bang.game.data.piece.TreeBed;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Controls the logging robots.
 */
public class LoggingRobotDelegate extends ScenarioDelegate
{
    /** Used by {@link #isWaveOver}. */
    public enum WaveAction { CONTINUE, END_WAVE, END_GAME };

    /**
     * Informs the delegate that the wave has started so that it can start sending in the robots.
     */
    public void waveStarted (BangObject bangobj, int wave, int difficulty, List<TreeBed> trees,
                             int treesGrown)
    {
        // compute our robot unit targets
        float ratio = BASE_ROBOT_RATIO + ROBOT_RATIO_INCREMENT * difficulty;
        float sratio = BASE_SUPER_RATIO + SUPER_RATIO_INCREMENT * difficulty;

        int units = (_bangmgr.getTeamSize() + 1) * _bangmgr.getPlayerCount();
        int rcount = Math.min(MAX_ROBOTS, Math.round(units * ratio)); // total bots
        int scount = Math.round(rcount * sratio); // super bots
        int ncount = rcount - scount; // non-super bots

        _target[LoggingRobot.LOCUST] = ncount / 2;
        _target[LoggingRobot.NORMAL] = ncount - _target[LoggingRobot.LOCUST];
        _target[LoggingRobot.SUPER_LOCUST] = scount / 2;
        _target[LoggingRobot.SUPER] = scount - _target[LoggingRobot.SUPER_LOCUST];

        // determine the rate at which robots respawn
        _rate = Math.min(BASE_RESPAWN_RATE + RESPAWN_RATE_INCREMENT * difficulty, 1f);

        // keep these around for later
        _wave = wave;
        _difficulty = difficulty;
        _ctrees = trees;
        _treesGrown = treesGrown;

        // spawn the next wave
        for (int ii = 0; ii < LoggingRobot.UNIT_TYPES.length; ii++) {
            _living[ii] = 0;
            spawnRobots(bangobj, ii, _target[ii]);
        }
    }

    /**
     * Called by the logging robot to determine whether the current wave should be ended.
     */
    public WaveAction isWaveOver (BangObject bangobj, short tick)
    {
        // count the living and fully grown trees
        int living = 0, grown = 0;
        for (TreeBed tree : _ctrees) {
            if (tree.isAlive()) {
                living++;
                if (tree.growth == TreeBed.FULLY_GROWN) {
                    grown++;
                }
            }
        }

        // if all the trees are dead, the game is over
        if (living == 0) {
            return WaveAction.END_GAME;
        }

        // if all the trees are grown...
        WaveAction action = WaveAction.CONTINUE;
        if (living == grown) {
            // ...stop spawning new robots
            _rate = 0f;
            // ...and when all robots are dead, end the wave
            if (IntListUtil.sum(_living) == 0) {
                action = WaveAction.END_WAVE;
            }
        }
        return action;
    }

    @Override // documentation inherited
    public void filterPieces (BangObject bangobj, Piece[] starts, List<Piece> pieces,
                              List<Piece> updates)
    {
        super.filterPieces(bangobj, starts, pieces, updates);

        // extract and remove all robot markers
        _robotSpots.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.ROBOTS)) {
                _robotSpots.add((Marker)p);
                iter.remove();
            }
        }
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj)
    {
        _logic = new LoggingRobotLogic();
        BangAI ai = new BangAI();
        ai.skill = 50; // TODO: adjust based on round difficulty?
        _logic.init(_bangmgr, -1, ai);
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        boolean validate = false;

        // update bots according to logic
        _logic.tick(bangobj.getPieceArray(), tick);

        // once all the trees are grown, we stop spawning robots
        if (_rate == 0f) {
            return validate;
        }

        for (int ii = 0; ii < LoggingRobot.UNIT_TYPES.length; ii++) {
            int delta = _target[ii] - _living[ii];
            if (delta <= 0) {
                _accum[ii] = 0f;
                continue;
            }

            // double the rate because we want that to be the average number of bots per tick
            _accum[ii] += (FastMath.nextRandomFloat() * _rate * delta * 2);
            int nbots = (int)_accum[ii];
            if (nbots > 0) {
                spawnRobots(bangobj, ii, nbots);
                _accum[ii] -= nbots;
                validate = true;
            }
        }
        return validate;

    }

    @Override // documentation inherited
    public void pieceWasKilled (BangObject bangobj, Piece piece, int shooter, int sidx)
    {
        // update type counts and record super robot kills
        if (piece instanceof LoggingRobot) {
            LoggingRobot bot = (LoggingRobot)piece;
            _living[bot.getRobotType()]--;
            if (bot.isSuper() && shooter != -1) {
                bangobj.stats[shooter].incrementStat(StatType.HARD_ROBOT_KILLS, 1);
            }
        }
    }

    protected void spawnRobots (BangObject bangobj, int type, int count)
    {
        Point[] bspots = findRobotSpawnPoints(bangobj, count);
        for (Point bspot : bspots) {
            if (bspot == null) {
                log.warning("Ran out of spawn spots for logging robots", "where", _bangmgr.where());
                return;
            }

            Unit unit = Unit.getUnit(LoggingRobot.UNIT_TYPES[type]);
            unit.assignPieceId(bangobj);
            unit.position(bspot.x, bspot.y);
            unit.lastActed += (bangobj.tick - 1);
            _bangmgr.addPiece(unit, AddPieceEffect.DROPPED);
            _living[type]++;
        }
    }

    /**
     * Finds and returns places to spawn logging robots based on the current aggression.
     */
    protected Point[] findRobotSpawnPoints (BangObject bangobj, int count)
    {
        // weight the spawn points
        int[] weights = new int[_robotSpots.size()];
        int wmax = 0;
        float aggression = computeAggression(bangobj);
        float absAggr = Math.abs(aggression);
        for (int ii = 0; ii < weights.length; ii++) {
            weights[ii] = weightSpawnPoint(bangobj, _robotSpots.get(ii), absAggr);
            wmax = Math.max(weights[ii], wmax);
        }
        if (aggression > 0f) { // invert weights for positive aggression
            for (int ii = 0; ii < weights.length; ii++) {
                weights[ii] = (wmax + 1) - weights[ii];
            }
        }

        // pick a markers by weight
        Point[] points = new Point[count];
        LoggingRobot dummy = new LoggingRobot();
        for (int ii = 0; ii < count; ) {
            int idx = RandomUtil.getWeightedIndex(weights);
            if (idx == -1) {
                return points;
            }

            Marker marker = _robotSpots.get(idx);
            points[ii] = bangobj.board.getOccupiableSpot(marker.x, marker.y, 3);
            if (points[ii] == null) {
                weights[idx] = -1;
            } else {
                bangobj.board.shadowPieceTemp(dummy, points[ii].x, points[ii].y);
                ii++;
            }
        }
        return points;
    }

    /**
     * Weights a spawn point according to the absolute value of the current aggression and the
     * point's distance from living trees.
     */
    protected int weightSpawnPoint (
        BangObject bangobj, Marker marker, float absAggr)
    {
        int total = 1;
        for (TreeBed tree : _ctrees) {
            if (!tree.isAlive() || tree.growth == 0) {
                continue;
            }
            int dist = tree.getDistance(marker), sdist = (int)Math.ceil(dist * absAggr),
                sgrowth = (int)Math.ceil(tree.growth * absAggr);
            total += (sdist * sgrowth);
        }
        return total;
    }

    /**
     * Computes the current aggression (from -1 to +1), which depends on the
     * number of living trees and the wave performance.
     */
    protected float computeAggression (BangObject bangobj)
    {
        int treePoints = _treesGrown * TreeBed.FULLY_GROWN;
        int maxPoints = _ctrees.size() * TreeBed.FULLY_GROWN * (_wave - 1);
        for (TreeBed tree : _ctrees) {
            maxPoints += TreeBed.FULLY_GROWN;
            if (tree.isAlive()) {
                treePoints += tree.growth;
            }
        }
        return 2f * treePoints / maxPoints - 1f;
    }

    /** Controls the behavior of the logging robots. */
    protected class LoggingRobotLogic extends AILogic
    {
        // documentation inherited
        public String getBigShotType ()
        {
            return null; // never called
        }

        // documentation inherited
        public String[] getUnitTypes (int count)
        {
            return null; // never called
        }

        @Override // documentation inherited
        protected void moveUnit (List<Piece> pieces, Unit unit, PointSet moves, PointSet attacks)
        {
            // find closest living tree, closest unit, closest teleporter
            TreeBed ctree = null;
            Unit cunit = null;
            Teleporter tporter = null;
            for (Piece piece : pieces) {
                if (piece instanceof TreeBed) {
                    TreeBed tree = (TreeBed)piece;
                    if (tree.growth > 0 && tree.isAlive() &&
                        (ctree == null || unit.getDistance(tree) < unit.getDistance(ctree))) {
                        ctree = tree;
                    }

                } else if (piece instanceof Unit &&
                           unit.validTarget(_bangobj, piece, false) &&
                           (cunit == null || unit.getDistance(piece) < unit.getDistance(cunit))) {
                    cunit = (Unit)piece;

                } else if (piece instanceof Teleporter &&
                           (tporter == null ||
                            unit.getDistance(piece) < unit.getDistance(tporter))) {
                    tporter = (Teleporter)piece;
                }
            }

            // if we're next to a living tree already, just shoot something
            if (ctree != null && unit.getDistance(ctree) == 1) {
                Piece target = getBestTarget(pieces, unit, unit.x, unit.y, _teval);
                if (target != null) {
                    executeOrder(unit, unit.x, unit.y, target);
                }

            // if there's a living tree, head towards it
            } else if (ctree != null &&
                       moveUnit(pieces, unit, moves, ctree.x, ctree.y, 1, _teval)) {
                return;

            // otherwise, head towards the closet unit
            } else if (cunit != null &&
                       moveUnit(pieces, unit, moves, cunit.x, cunit.y, -1, _teval)) {
                return;

            // or the closest teleporter
            } else if (tporter != null &&
                       moveUnit(pieces, unit, moves, tporter.x, tporter.y, 0, _teval)) {
                return;
            }
        }

        /** Ranks potential targets by the amount of damage the unit will do, and the amount of
         * damage the target has already taken.  Trees are better than anything. */
        protected TargetEvaluator _teval = new TargetEvaluator() {
            public int getWeight (BangObject bangobj, Unit unit, Piece target, int dist,
                                  PointSet preferredMoves) {
                return (target instanceof TreeBed ? 100000 : 0) +
                    unit.computeScaledDamage(bangobj, target, 1f) * 100 + target.damage;
            }
        };
    }

    /** The index of the current wave. */
    protected int _wave;

    /** The difficulty level of the current wave. */
    protected int _difficulty;

    /** The accumulated number of trees grown in completed waves. */
    protected int _treesGrown;

    /** The logic used to control the robots. */
    protected LoggingRobotLogic _logic;

    /** The trees that are on the board during the current wave. */
    protected List<TreeBed> _ctrees = Lists.newArrayList();

    /** The number of living robots of each type and the target numbers. */
    protected int[] _living = new int[LoggingRobot.UNIT_TYPES.length],
        _target = new int[LoggingRobot.UNIT_TYPES.length];

    /** The rate at which robots respawn and the accumulators. */
    protected float _rate;
    protected float[] _accum = new float[LoggingRobot.UNIT_TYPES.length];

    /** The spots from which robots emerge. */
    protected List<Marker> _robotSpots = Lists.newArrayList();

    /** The base rate at which logging robots respawn (bots per tick). */
    protected static final float BASE_RESPAWN_RATE = 1 / 8f;

    /** The increment in bots per tick for each wave. */
    protected static final float RESPAWN_RATE_INCREMENT = 3 / 72f;

    /** The base number of logging robots to keep alive per unit. */
    protected static final float BASE_ROBOT_RATIO = 1 / 3f;

    /** The increment in number of logging robots per unit for each wave. */
    protected static final float ROBOT_RATIO_INCREMENT = 1 / 24f;

    /** The maximum number of logging robots on the board. */
    protected static final int MAX_ROBOTS = 6;

    /** The base proportion of high-powered robots. */
    protected static final float BASE_SUPER_RATIO = 0f;

    /** The proportionate increment of high-powered robots. */
    protected static final float SUPER_RATIO_INCREMENT = 1 / 9f;
}
