//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import com.jme.math.FastMath;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.presents.server.InvocationException;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.ClearPieceEffect;
import com.threerings.bang.game.data.effect.MarqueeEffect;
import com.threerings.bang.game.data.effect.TreeBedEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.LoggingRobot;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
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
        registerDelegate(new RespawnDelegate(8));
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
    }
    
    @Override // documentation inherited
    public void tick (BangObject bangobj, short tick)
    {
        super.tick(bangobj, tick);
        
        // the first wave starts four ticks in
        if (_wave == 0 && tick >= FIRST_WAVE_TICK) {
            startNextWave(bangobj);
            return;
        }
        
        int living = 0, grown = 0;
        for (TreeBed tree : _trees) {
            if (tree.isAlive()) {
                living++;
                if (tree.growth == TreeBed.FULLY_GROWN) {
                    grown++;
                }
            }
        }
        
        // if half of the trees are dead, the game is over
        if (living < _trees.size() / 2) {
            bangobj.setLastTick(tick);
            
        // if all living trees are fully grown, count down towards ending the
        // wave
        } else if (living == grown) {
            if ((++_grownTicks) >= NEXT_WAVE_TICKS) {
                endWave(bangobj, tick, grown);
            }
            
        } else {
            _grownTicks = 0;
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
            maxPoints = _trees.size() * TreeBed.FULLY_GROWN * (_wave - 1);
        for (TreeBed tree : _trees) {
            if (tree.growth == 0) {
                continue;
            }
            maxPoints += tree.growth;
            if (!tree.isAlive()) {
                continue;
            }
            treePoints += tree.growth;
            for (int ii = 0; ii < bangobj.stats.length; ii++) {
                bangobj.stats[ii].incrementStat(
                    ForestGuardiansInfo.GROWTH_STATS[tree.growth-1], 1);
                bangobj.grantPoints(ii,
                    ForestGuardiansInfo.GROWTH_POINTS[tree.growth-1]);
            }
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
                ii++;
            }
        }
        return points;
    }
    
    /**
     * Computes the current aggression (from -1 to +1), which depends on the
     * number of living trees.
     */
    protected float computeAggression (BangObject bangobj)
    {
        int treePoints = 0, maxPoints = 0;
        for (TreeBed tree : _trees) {
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
        for (TreeBed tree : _trees) {
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
    protected void endWave (BangObject bangobj, short tick, int grown)
    {
        // notify the delegate
        _lrdelegate.waveEnded(bangobj);
        
        // if there isn't time to start another wave, end the game
        if (bangobj.lastTick - tick < MIN_WAVE_TICKS) {
            bangobj.setLastTick(tick);
            return;
        }
        
        // record stats and points for trees grown, add the score to the
        // total, and start the next wave
        for (int ii = 0; ii < bangobj.stats.length; ii++) {
            bangobj.stats[ii].incrementStat(Stat.Type.TREES_ELDER, grown);
            bangobj.grantPoints(ii, ForestGuardiansInfo.GROWTH_POINTS[
                TreeBed.FULLY_GROWN - 1] * grown);
        }
        _wavePoints += grown;
        startNextWave(bangobj);
    }
    
    /**
     * Starts the next wave of logging robots.
     */
    protected void startNextWave (BangObject bangobj)
    {
        String msg;
        if ((++_wave) < 10) {
            msg = "m.nth." + _wave;
        } else {
            msg = MessageBundle.compose("m.nth.ten_plus", _wave);
        }
        _bangmgr.deployEffect(-1, new MarqueeEffect(
            MessageBundle.compose("m.wave", msg)));
        
        // reset all of the trees for waves after the first
        if (_wave > 1) {
            for (TreeBed tree : _trees) {
                _bangmgr.deployEffect(-1, new TreeBedEffect(tree, 50, 0));
            }
        }
        
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
                ROBOT_RATIO_INCREMENT * (_wave - 1);
            _target = (int)Math.round((_bangmgr.getTeamSize() + 1) *
                _bangmgr.getPlayerCount() * ratio);
            spawnRobots(bangobj, _target);
            
            // determine the rate at which robots respawn
            _rate = BASE_RESPAWN_RATE + RESPAWN_RATE_INCREMENT * (_wave - 1);
        }
        
        public void waveEnded (BangObject bangobj)
        {
            // remove remaining members of the current wave, living or dead
            Piece[] pieces = bangobj.getPieceArray();
            for (Piece piece : pieces) {
                if (piece instanceof LoggingRobot) {
                    _bangmgr.deployEffect(-1, new ClearPieceEffect(piece));
                }
            }
            _living = 0;
        }
        
        @Override // documentation inherited
        public void tick (BangObject bangobj, short tick)
        {
            // update bots according to logic
            _logic.tick(bangobj.getPieceArray(), tick);
            
            // consider spawning more bots
            if (_living < _target) {
                // double the rate because we want that to be the average
                // number of bots per tick
                _accum += (FastMath.nextRandomFloat() * _rate * 2f);
                int nbots = (int)_accum;
                if (nbots > 0) {
                    spawnRobots(bangobj, nbots);
                    _accum -= nbots;
                }
            } else {
                _accum = 0f;
            }
        }
        
        @Override // documentation inherited
        public void pieceWasKilled (BangObject bangobj, Piece piece)
        {
            if (piece instanceof LoggingRobot) {
                _living--;
            }
        }
        
        protected void spawnRobots (BangObject bangobj, int count)
        {
            Point[] bspots = findRobotSpawnPoints(bangobj, count);
            for (Point bspot : bspots) {
                if (bspot == null) {
                    log.warning("Ran out of spawn spots for logging robots " +
                        "[where=" + _bangmgr.where() + "]");
                    return;
                }
                Unit unit = Unit.getUnit(
                    RandomUtil.pickRandom(LoggingRobot.UNIT_TYPES));
                unit.assignPieceId(bangobj);
                unit.position(bspot.x, bspot.y);
                _bangmgr.addPiece(unit, (bangobj.tick >= 0) ?
                    AddPieceEffect.DROPPED : null);
                _living++;
            }
        }
        
        /** The logic used to control the robots. */
        protected LoggingRobotLogic _logic;
        
        /** The number of living robots and the target number. */
        protected int _living, _target;
        
        /** The rate at which robots respawn and the accumulator. */
        protected float _rate, _accum;
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
            // find closest living tree, closet unit
            TreeBed ctree = null;
            Unit cunit = null;
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
                ctree.y, _teval)) {
                return;
            
            // otherwise, head towards the closet unit
            } else if (cunit != null && moveUnit(pieces, unit, moves, cunit.x,
                cunit.y, _teval)) {
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
    
    /** The delegate that spawns and controls logging robots. */
    protected LoggingRobotDelegate _lrdelegate;
    
    /** The spots from which robots emerge. */
    protected ArrayList<Marker> _robotSpots = new ArrayList<Marker>();
    
    /** The tree beds on the board. */
    protected ArrayList<TreeBed> _trees = new ArrayList<TreeBed>();
    
    /** The payouts for each player, determined at the end of the round. */
    protected int[] _payouts;

    /** Used to track the spawn locations for the fetishes. */
    protected PointSet _fetishSpots = new PointSet();
    
    /** The current wave of robots. */
    protected int _wave;
    
    /** The number of points accumulated in the completed waves. */
    protected int _wavePoints;
    
    /** The number of ticks that the players have held the trees at full
     * growth. */
    protected int _grownTicks;
    
    /** The tick at which to activate the first wave. */
    protected static final int FIRST_WAVE_TICK = 2;
    
    /** The number of ticks the players must hold the trees at full growth in
     * order to bring on the next wave. */
    protected static final int NEXT_WAVE_TICKS = 2;
    
    /** The number of remaining ticks required to start another wave. */
    protected static final int MIN_WAVE_TICKS = 16;
    
    /** The base number of logging robots to keep alive per unit. */
    protected static final float BASE_ROBOT_RATIO = 1 / 3f;
    
    /** The increment in number of logging robots per unit for each wave. */
    protected static final float ROBOT_RATIO_INCREMENT = 1 / 6f;
    
    /** The base rate at which logging robots respawn (bots per tick). */
    protected static final float BASE_RESPAWN_RATE = 0.25f;
    
    /** The increment in bots per tick for each wave. */
    protected static final float RESPAWN_RATE_INCREMENT = 0.25f;
    
    /** Payout adjustments for rankings in 2/3/4 player games. */
    protected static final int[][] PAYOUT_ADJUSTMENTS = {
        { -10, +10 }, { -10, 0, +10 }, { -10, -5, +5, +10 } };
}
