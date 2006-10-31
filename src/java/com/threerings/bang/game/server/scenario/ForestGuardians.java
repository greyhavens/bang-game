//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.jme.math.FastMath;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.QuickSort;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.presents.server.InvocationException;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Rating;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.ClearPieceEffect;
import com.threerings.bang.game.data.effect.RobotWaveEffect;
import com.threerings.bang.game.data.effect.TreeBedEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.LoggingRobot;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Teleporter;
import com.threerings.bang.game.data.piece.TreeBed;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.scenario.ForestGuardiansInfo;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.ForestGuardiansLogic;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li> Trees sprout from fixed locations on the board.
 * <li> Players surround the trees to make them grow.
 * <li> Logging robots attempt to cut down the trees.
 * <li> Four fetishes grant special powers to the units that hold them.
 * <li> The round ends after a fixed time limit.
 * <li> Players earn money for all trees alive at the end of the round.
 * </ul>
 */
public class ForestGuardians extends Scenario
{
    public ForestGuardians ()
    {
        registerDelegate(new TrainDelegate());
        registerDelegate(_rdelegate = new RespawnDelegate(8));
        registerDelegate(_lrdelegate = new LoggingRobotDelegate());
    }
    
    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new ForestGuardiansLogic();
    }
    
    @Override // documentation inherited
    public void filterPieces (
        BangObject bangobj, ArrayList<Piece> starts, ArrayList<Piece> pieces,
        ArrayList<Piece> updates)
    {
        super.filterPieces(bangobj, starts, pieces, updates);

        // extract and remove all robot markers; store the tree beds
        _robotSpots.clear();
        _fetishSpots.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.ROBOTS)) {
                _robotSpots.add((Marker)p);
                iter.remove();
            } else if (Marker.isMarker(p, Marker.FETISH)) {
                _fetishSpots.add(p.x, p.y);
                iter.remove();
            } else if (p instanceof TreeBed) {
                TreeBed tree = (TreeBed)p;
                _trees.add(tree);
            }
        }
    }
    
    @Override // documentation inherited    
    public int modifyDamageDone (int pidx, int tidx, int ddone)
    {
        // points are granted for shooting the robots; subtracted for shooting
        // your teammates
        return (tidx == -1) ? ddone : (-3 * ddone / 2);
    }
    
    @Override // documentation inherited    
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> starts,
                                PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);
        
        // place the four fetishes
        Piece[] pieces = bangobj.getPieceArray();
        placeBonus(bangobj, pieces,
            Bonus.createBonus("indian_post/fetish_bear"), _fetishSpots);
        placeBonus(bangobj, pieces,
            Bonus.createBonus("indian_post/fetish_fox"), _fetishSpots);
        placeBonus(bangobj, pieces,
            Bonus.createBonus("indian_post/fetish_frog"), _fetishSpots);
        placeBonus(bangobj, pieces,
            Bonus.createBonus("indian_post/fetish_turtle"), _fetishSpots);
    
        // set the initial difficulty level based on the average player rating
        int nplayers = _bangmgr.getPlayerSlots(), trating = 0;
        for (int ii = 0; ii < nplayers; ii++) {
            trating += _bangmgr.getPlayerRecord(ii).getRating(
                ForestGuardiansInfo.IDENT).rating;
        }
        float rratio = ((float)(trating / nplayers) - Rating.DEFAULT_RATING) /
            (Rating.MAXIMUM_RATING - Rating.DEFAULT_RATING);
        _difficulty = (int)Math.max(0,
            Math.round(rratio * MAX_INITIAL_DIFFICULTY));
        
        // remove all but a random subset of the tree beds
        resetTrees(bangobj);
        
        // start the first wave on the second tick
        _nextWaveTick = NEXT_WAVE_TICKS;
    }
    
    @Override // documentation inherited
    public void tick (BangObject bangobj, short tick)
    {
        // don't do anything until the wave has started
        super.tick(bangobj, tick);
        if (tick < _nextWaveTick) {
            return;
        } else if (tick == _nextWaveTick) {
            startNextWave(bangobj);
        }
        
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
            bangobj.setLastTick(tick);
        
        // when all trees are fully grown and there are no more
        // logging robots, the logging robot delegate will end the
        // wave   
        } else {
            _allGrown = (living == grown);
        }
    }
    
    @Override // documentation inherited
    public void roundDidEnd (BangObject bangobj)
    {
        // nothing to do if we end before the first wave
        super.roundDidEnd(bangobj);
        if (_wave < 1) {
            return;
        }
        
        // at the end of the game, grant points for all trees still alive in
        // proportion to their growth so as not to penalize players if a
        // wave ends early because the clock ran out.
        int treePoints = _wavePoints * TreeBed.FULLY_GROWN,
            maxPoints = _waveMax * TreeBed.FULLY_GROWN,
            living = 0, total = 0;
        for (TreeBed tree : _ctrees) {
            if (tree.growth == 0) {
                continue;
            }
            total++;
            maxPoints += tree.growth;
            if (!tree.isAlive()) {
                continue;
            }
            living++;
            treePoints += tree.growth;
            int points = scalePoints(
                ForestGuardiansInfo.GROWTH_POINTS[tree.growth-1]);
            for (int ii = 0; ii < bangobj.stats.length; ii++) {
                bangobj.stats[ii].incrementStat(
                    ForestGuardiansInfo.GROWTH_STATS[tree.growth-1], 1);
                bangobj.stats[ii].incrementStat(Stat.Type.WAVE_POINTS, points);
                bangobj.grantPoints(ii, points);
            }
        }
        int perf = (total > 0 ? RobotWaveEffect.getPerformance(living, total) :
            RobotWaveEffect.MAX_PERFORMANCE);
        for (StatSet stats : bangobj.stats) {
            stats.appendStat(Stat.Type.WAVE_SCORES, perf);
        }
        
        _payouts = new int[bangobj.players.length];
        Arrays.fill(_payouts, 60 + (95 - 60) * treePoints / maxPoints);
        
        int[] points = bangobj.perRoundPoints[bangobj.roundId-1],
            spoints = new ArrayIntSet(points).toIntArray();
        if (spoints.length < 2) {
            return;
        }
        for (int ii = 0; ii < _payouts.length; ii++) {
            int ppoints = points[ii];
            _payouts[ii] += PAYOUT_ADJUSTMENTS[spoints.length-2][
                IntListUtil.indexOf(spoints, ppoints)] /
                    count(points, ppoints);
        }
    }

    @Override // documentation inherited
    public void recordStats (
        BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(bangobj, gameTime, pidx, user);

        // record the number of trees they grew
        for (int ii = 0; ii < ForestGuardiansInfo.GROWTH_STATS.length; ii++) {
            Stat.Type stat = ForestGuardiansInfo.GROWTH_STATS[ii];
            int grown = bangobj.stats[pidx].getIntStat(stat);
            if (grown > 0) {
                user.stats.incrementStat(stat, grown);
            }
        }
        
        // and the number of super robots killed
        int kills = bangobj.stats[pidx].getIntStat(Stat.Type.HARD_ROBOT_KILLS);
        if (kills > 0) {
            user.stats.incrementStat(Stat.Type.HARD_ROBOT_KILLS, kills);
        }
        
        // and the number of perfect waves
        int waves = count(
            bangobj.stats[pidx].getIntArrayStat(Stat.Type.WAVE_SCORES),
            RobotWaveEffect.MAX_PERFORMANCE);
        if (waves > 0) {
            user.stats.incrementStat(Stat.Type.PERFECT_WAVES, waves);
        }
        
        // and the highest difficulty level reached
        user.stats.maxStat(Stat.Type.HIGHEST_SAWS, _difficulty + 1);
    }
    
    @Override // documentation inherited
    public int computeEarnings (
        BangObject bangobj, int pidx, int ridx,
        BangManager.PlayerRecord[] precords, BangManager.RankRecord[] ranks,
        BangManager.RoundRecord[] rounds)
    {
        return (_payouts == null) ? 0 : _payouts[pidx];
    }
    
    @Override // documentation inherited
    protected long getBaseTickTime ()
    {
        // we have fewer units, but we don't want crazy speed
        return BASE_TICK_TIME + 1000L;
    }
    
    /**
     * Finds and returns places to spawn logging robots based on the current
     * aggression.
     */
    protected Point[] findRobotSpawnPoints (BangObject bangobj, int count)
    {
        // weight the spawn points
        int[] weights = new int[_robotSpots.size()];
        int wmax = 0;
        float aggression = computeAggression(bangobj),
            absAggr = Math.abs(aggression);
        for (int ii = 0; ii < weights.length; ii++) {
            weights[ii] = weightSpawnPoint(bangobj, _robotSpots.get(ii),
                absAggr);
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
            points[ii] = bangobj.board.getOccupiableSpot(
                marker.x, marker.y, 3);
            if (points[ii] == null) {
                weights[idx] = -1;
            } else {
                bangobj.board.shadowPieceTemp(dummy,
                    points[ii].x, points[ii].y);
                ii++;
            }
        }
        return points;
    }
    
    /**
     * Computes the current aggression (from -1 to +1), which depends on the
     * number of living trees and the wave performance.
     */
    protected float computeAggression (BangObject bangobj)
    {
        int treePoints = _wavePoints * TreeBed.FULLY_GROWN,
            maxPoints = _ctrees.size() * TreeBed.FULLY_GROWN * (_wave - 1);
        for (TreeBed tree : _ctrees) {
            maxPoints += TreeBed.FULLY_GROWN;
            if (tree.isAlive()) {
                treePoints += tree.growth;
            }
        }
        return 2f * treePoints / maxPoints - 1f;
    }
    
    /**
     * Weights a spawn point according to the absolute value of the current
     * aggression and the point's distance from living trees.
     */
    protected int weightSpawnPoint (
        BangObject bangobj, Marker marker, float absAggr)
    {
        int total = 1;
        for (TreeBed tree : _ctrees) {
            if (!tree.isAlive() || tree.growth == 0) {
                continue;
            }
            int dist = tree.getDistance(marker),
                sdist = (int)Math.ceil(dist * absAggr),
                sgrowth = (int)Math.ceil(tree.growth * absAggr);
            total += (sdist * sgrowth);
        }
        return total;
    }
    
    /**
     * Ends the current wave and either starts the next or ends the game.
     */
    protected void endWave (BangObject bangobj, short tick)
    {
        // if there isn't time to start another wave, end the game
        if (bangobj.lastTick - tick < MIN_WAVE_TICKS) {
            bangobj.setLastTick(tick);
            return;
        }
        
        // clear all advance orders
        _bangmgr.clearOrders();
        
        // announce the end of the wave
        RobotWaveEffect rweffect = new RobotWaveEffect(_wave);
        _bangmgr.deployEffect(-1, rweffect);
        int grown = rweffect.living, perf = rweffect.getPerformance();
        
        // record stats and points for trees grown, add the score to the
        // total
        int points = scalePoints(ForestGuardiansInfo.GROWTH_POINTS[
            TreeBed.FULLY_GROWN - 1] * grown);
        for (int ii = 0; ii < bangobj.stats.length; ii++) {
            bangobj.stats[ii].incrementStat(Stat.Type.TREES_ELDER, grown);
            bangobj.stats[ii].appendStat(Stat.Type.WAVE_SCORES, perf);
            bangobj.stats[ii].incrementStat(Stat.Type.WAVE_POINTS, points);
            bangobj.grantPoints(ii, points);
        }
        _wavePoints += grown;
        _waveMax += _ctrees.size();
        
        // if at least half the trees were saved, increase the difficulty
        // level
        if (grown >= _ctrees.size() / 2) {
            _difficulty = Math.min(_difficulty + 1, MAX_DIFFICULTY);
        }
        
        // reset the trees and choose a new subset
        resetTrees(bangobj);
        
        // respawn all player units on the next tick
        _rdelegate.respawnAll(tick);
        
        // count down towards starting the next wave
        _nextWaveTick = tick + NEXT_WAVE_TICKS;
    }
    
    /**
     * Scales a base number of points according to the current difficulty
     * level.
     */
    protected int scalePoints (int base)
    {
        return (int)(base * (1f + _difficulty * POINT_MULTIPLIER_INCREMENT));
    }
    
    /**
     * Adds and removes trees such that the board contains a random subset
     * of them.
     */
    protected void resetTrees (final BangObject bangobj)
    {
        // shuffle the trees and move any currently visible to the end
        Collections.shuffle(_trees);
        QuickSort.sort(_trees, new Comparator<TreeBed>() {
            public int compare (TreeBed t1, TreeBed t2) {
                boolean c1 = bangobj.pieces.contains(t1),
                    c2 = bangobj.pieces.contains(t2);
                return (c1 == c2) ? 0 : (c1 ? +1 : -1);
            }
        });
        
        // determine the desired number of trees and add/remove accordingly
        float ratio = BASE_TREE_RATIO + TREE_RATIO_INCREMENT * _difficulty;
        int ntrees = Math.min(MAX_TREES,
            (int)Math.round(getUnitTotal() * ratio));
        _ctrees.clear();
        for (TreeBed tree : _trees) {
            if (ntrees-- > 0) {
                if (!bangobj.pieces.contains(tree)) {
                    addTree(bangobj, tree);
                }
                // reset and retrieve the cloned instance
                _bangmgr.deployEffect(-1, new TreeBedEffect(tree));
                _ctrees.add((TreeBed)bangobj.pieces.get(tree.pieceId));
                
            } else {
                if (bangobj.pieces.contains(tree)) {
                    _bangmgr.deployEffect(-1, new ClearPieceEffect(tree));
                }
            }
        }
        _allGrown = false;
    }
    
    /**
     * Returns the total number of player units, living or dead, including
     * Big Shots.
     */
    protected int getUnitTotal ()
    {
        return (_bangmgr.getTeamSize() + 1) * _bangmgr.getPlayerCount();
    }
    
    /**
     * Adds a tree (back) to the board, moving any unit occupying its space.
     */
    protected void addTree (BangObject bangobj, TreeBed tree)
    {
        if (!bangobj.board.isOccupiable(tree.x, tree.y)) {
            for (Piece piece : bangobj.pieces) {
                if (piece instanceof Unit && piece.intersects(tree)) {
                    Point spot = bangobj.board.getOccupiableSpot(
                        tree.x, tree.y, 3);
                    if (spot != null) {
                        _bangmgr.deployEffect(-1,
                            ((Unit)piece).generateMoveEffect(
                                bangobj, spot.x, spot.y, null));
                    } else {
                        log.warning("Unable to find spot to move unit " +
                            "[unit=" + piece + "].");
                    }
                }
            }
        }
        _bangmgr.addPiece(tree);
    }
    
    /**
     * Starts the next wave of logging robots.
     */
    protected void startNextWave (BangObject bangobj)
    {
        // announce the start of the wave
        _bangmgr.deployEffect(-1, new RobotWaveEffect(++_wave, _difficulty));
        
        // notify the delegate
        _lrdelegate.waveStarted(bangobj);
    }
    
    /**
     * Returns the number of times the value appears in the array.
     */
    protected static int count (int[] values, int value)
    {
        int count = 0;
        for (int ii = 0; ii < values.length; ii++) {
            if (values[ii] == value) {
                count++;
            }
        }
        return count;
    }
    
    /** Controls the logging robots. */
    protected class LoggingRobotDelegate extends ScenarioDelegate
    {
        @Override // documentation inherited
        public void roundWillStart (BangObject bangobj)
        {
            _logic = new LoggingRobotLogic();
            _logic.init(_bangmgr, -1);
        }
        
        public void waveStarted (BangObject bangobj)
        {
            // spawn the next wave
            float ratio = BASE_ROBOT_RATIO +
                    ROBOT_RATIO_INCREMENT * _difficulty,
                sratio = BASE_SUPER_RATIO +
                    SUPER_RATIO_INCREMENT * _difficulty;
            int rcount = Math.min(MAX_ROBOTS,
                (int)Math.round(getUnitTotal() * ratio)), // total bots
                scount = (int)Math.round(rcount * sratio), // super bots
                ncount = rcount - scount; // non-super bots
            _target[LoggingRobot.LOCUST] = ncount / 2;
            _target[LoggingRobot.NORMAL] =
                ncount - _target[LoggingRobot.LOCUST];
            _target[LoggingRobot.SUPER_LOCUST] = scount / 2;
            _target[LoggingRobot.SUPER] =
                scount - _target[LoggingRobot.SUPER_LOCUST];
            for (int ii = 0; ii < LoggingRobot.UNIT_TYPES.length; ii++) {
                _living[ii] = 0;
                spawnRobots(bangobj, ii, _target[ii]);
            }
            
            // determine the rate at which robots respawn
            _rate = Math.min(BASE_RESPAWN_RATE +
                RESPAWN_RATE_INCREMENT * _difficulty, 1f);
        }
        
        @Override // documentation inherited
        public void tick (BangObject bangobj, short tick)
        {
            // don't do anything between waves
            if (tick <= _nextWaveTick) {
                return;

            // if all trees are grown and all bots are dead, end the wave
            } else if (_allGrown && IntListUtil.sum(_living) == 0) {
                endWave(bangobj, tick);
                return;
            }
            
            // update bots according to logic
            _logic.tick(bangobj.getPieceArray(), tick);
            
            // consider spawning more bots of each type if the trees are still
            // growing
            if (_allGrown) {
                return;
            }
            for (int ii = 0; ii < LoggingRobot.UNIT_TYPES.length; ii++) {
                int delta = _target[ii] - _living[ii];
                if (delta <= 0) {
                    _accum[ii] = 0f;
                    continue;
                }
                // double the rate because we want that to be the average
                // number of bots per tick
                int nbots = (int)(_accum[ii] +=
                    (FastMath.nextRandomFloat() * _rate * delta * 2));
                if (nbots > 0) {
                    spawnRobots(bangobj, ii, nbots);
                    _accum[ii] -= nbots;
                }
            }
        }
        
        @Override // documentation inherited
        public void pieceWasKilled (
            BangObject bangobj, Piece piece, int shooter)
        {
            // update type counts and record super robot kills
            if (piece instanceof LoggingRobot) {
                LoggingRobot bot = (LoggingRobot)piece;
                _living[bot.getRobotType()]--;
                if (bot.isSuper() && shooter != -1) {
                    bangobj.stats[shooter].incrementStat(
                        Stat.Type.HARD_ROBOT_KILLS, 1);
                }
            }
        }
        
        protected void spawnRobots (BangObject bangobj, int type, int count)
        {
            Point[] bspots = findRobotSpawnPoints(bangobj, count);
            for (Point bspot : bspots) {
                if (bspot == null) {
                    log.warning("Ran out of spawn spots for logging robots " +
                        "[where=" + _bangmgr.where() + "]");
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
        
        /** The logic used to control the robots. */
        protected LoggingRobotLogic _logic;
        
        /** The number of living robots of each type and the target numbers. */
        protected int[] _living = new int[LoggingRobot.UNIT_TYPES.length],
            _target = new int[LoggingRobot.UNIT_TYPES.length];
        
        /** The rate at which robots respawn and the accumulators. */
        protected float _rate;
        protected float[] _accum = new float[LoggingRobot.UNIT_TYPES.length];
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
        protected void moveUnit (
            Piece[] pieces, Unit unit, PointSet moves, PointSet attacks)
        {
            // find closest living tree, closest unit, closest teleporter
            TreeBed ctree = null;
            Unit cunit = null;
            Teleporter tporter = null;
            for (Piece piece : pieces) {
                if (piece instanceof TreeBed) {
                    TreeBed tree = (TreeBed)piece;
                    if (tree.growth > 0 && tree.isAlive() &&
                        (ctree == null || unit.getDistance(tree) <
                            unit.getDistance(ctree))) {
                        ctree = tree;
                    }
                } else if (piece instanceof Unit &&
                    unit.validTarget(_bangobj, piece, false) &&
                        (cunit == null || unit.getDistance(piece) <
                            unit.getDistance(cunit))) {
                    cunit = (Unit)piece;
                    
                } else if (piece instanceof Teleporter &&
                    (tporter == null || unit.getDistance(piece) <
                        unit.getDistance(tporter))) {
                    tporter = (Teleporter)piece;
                }
            }
            
            // if we're next to a living tree already, just shoot something
            if (ctree != null && unit.getDistance(ctree) == 1) {
                Piece target = getBestTarget(pieces, unit, unit.x, unit.y,
                    _teval);
                if (target != null) {
                    executeOrder(unit, unit.x, unit.y, target);
                }
            
            // if there's a living tree, head towards it
            } else if (ctree != null && moveUnit(pieces, unit, moves, ctree.x,
                ctree.y, 1, _teval)) {
                return;
            
            // otherwise, head towards the closet unit
            } else if (cunit != null && moveUnit(pieces, unit, moves, cunit.x,
                cunit.y, -1, _teval)) {
                return;
            
            // or the closest teleporter
            } else if (tporter != null && moveUnit(pieces, unit, moves,
                tporter.x, tporter.y, 0, _teval)) {
                return;
            }
        }
        
        /** Ranks potential targets by the amount of damage the unit will do,
         * and the amount of damage the target has already taken.  Trees are
         * better than anything. */
        protected TargetEvaluator _teval = new TargetEvaluator() {
            public int getWeight (BangObject bangobj, Unit unit, Piece target, 
                    int dist, PointSet preferredMoves) {
                return (target instanceof TreeBed ? 100000 : 0) +
                    unit.computeScaledDamage(bangobj, target, 1f) * 100 +
                    target.damage;
            }
        };
    }
    
    /** The delegate responsible for respawning player units. */
    protected RespawnDelegate _rdelegate;
    
    /** The delegate that spawns and controls logging robots. */
    protected LoggingRobotDelegate _lrdelegate;
    
    /** The spots from which robots emerge. */
    protected ArrayList<Marker> _robotSpots = new ArrayList<Marker>();
    
    /** The tree beds on the board to begin with and the trees that are
     * currently on the board. */
    protected ArrayList<TreeBed> _trees = new ArrayList<TreeBed>(),
        _ctrees = new ArrayList<TreeBed>();
    
    /** The payouts for each player, determined at the end of the round. */
    protected int[] _payouts;

    /** Used to track the spawn locations for the fetishes. */
    protected PointSet _fetishSpots = new PointSet();
    
    /** The current wave of robots. */
    protected int _wave;
    
    /** The difficulty level of the current wave. */
    protected int _difficulty;
    
    /** The number of points accumulated in the completed waves. */
    protected int _wavePoints;
    
    /** The possible number of points that could have been accumulated. */
    protected int _waveMax;
    
    /** The tick at which to start the next wave. */
    protected int _nextWaveTick;
    
    /** Set when all living trees are fully grown. */
    protected boolean _allGrown;
    
    /** The number of ticks the players must hold the trees at full growth in
     * order to bring on the next wave. */
    protected static final int NEXT_WAVE_TICKS = 2;
    
    /** The number of remaining ticks required to start another wave. */
    protected static final int MIN_WAVE_TICKS = 16;
    
    /** The maximum initial difficulty level (set when the average player
     * rating is very high). */
    protected static final int MAX_INITIAL_DIFFICULTY = 4;
    
    /** The maximum attainable difficulty level. */
    protected static final int MAX_DIFFICULTY = 9;
    
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
    
    /** The base number of tree beds to create per unit. */
    protected static final float BASE_TREE_RATIO = 1 / 3f;
    
    /** The increment in number of trees per unit for each wave. */
    protected static final float TREE_RATIO_INCREMENT = 1 / 24f;
    
    /** The maximum number of trees on the board. */
    protected static final int MAX_TREES = 8;
    
    /** The base rate at which logging robots respawn (bots per tick). */
    protected static final float BASE_RESPAWN_RATE = 1 / 8f;
    
    /** The increment in bots per tick for each wave. */
    protected static final float RESPAWN_RATE_INCREMENT = 3 / 72f;
    
    /** The increment in the point multiplier (which starts at 1) for each
     * difficulty level. */
    protected static final float POINT_MULTIPLIER_INCREMENT = 1 / 9f;
    
    /** Payout adjustments for rankings in 2/3/4 player games. */
    protected static final int[][] PAYOUT_ADJUSTMENTS = {
        { -10, +10 }, { -10, 0, +10 }, { -10, -5, +5, +10 } };
}
