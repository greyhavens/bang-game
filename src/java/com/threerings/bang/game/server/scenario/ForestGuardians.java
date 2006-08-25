//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.TreeBedEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.LoggingRobot;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TreeBed;
import com.threerings.bang.game.data.piece.Unit;
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
        registerDelegate(new RespawnDelegate());
        registerDelegate(new LoggingRobotDelegate());
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

        // extract and remove all robot markers; store the tree beds and put
        // them in random growth states
        _robotSpots.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.ROBOTS)) {
                _robotSpots.add((Marker)p);
                iter.remove();
            } else if (p instanceof TreeBed) {
                TreeBed tree = (TreeBed)p;
                _trees.add(tree); 
                tree.growth = (byte)RandomUtil.getInt(TreeBed.FULLY_GROWN + 1);
                updates.add(tree);
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
            Bonus.createBonus("indian_post/fetish_bear"), _bonusSpots);
        placeBonus(bangobj, pieces,
            Bonus.createBonus("indian_post/fetish_fox"), _bonusSpots);
        placeBonus(bangobj, pieces,
            Bonus.createBonus("indian_post/fetish_frog"), _bonusSpots);
        placeBonus(bangobj, pieces,
            Bonus.createBonus("indian_post/fetish_turtle"), _bonusSpots);
    }
    
    @Override // documentation inherited
    public void roundDidEnd (BangObject bangobj)
    {
        super.roundDidEnd(bangobj);

        // note trees grown
        int treePoints = 0, maxPoints = 0;
        for (TreeBed tree : _trees) {
            maxPoints += TreeBed.FULLY_GROWN;
            if (tree.isAlive()) {
                treePoints += tree.growth;
                if (tree.growth > 0) {
                    for (StatSet stats : bangobj.stats) {
                        stats.incrementStat(Stat.Type.TREES_GROWN, 1);
                    }
                }
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
        int grown = bangobj.stats[pidx].getIntStat(Stat.Type.TREES_GROWN);
        if (grown > 0) {
            user.stats.incrementStat(Stat.Type.TREES_GROWN, grown);
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
            _rlogic = new LoggingRobotLogic();
            _rlogic.init(_bangmgr, -1);
            
            // spawn initial bots
            _rtarget = (int)((_bangmgr.getTeamSize() + 1) *
                _bangmgr.getPlayerCount() * LOGGING_ROBOTS_PER_UNIT);
            spawnRobots(bangobj, _rtarget);
        }
        
        @Override // documentation inherited
        public void tick (BangObject bangobj, short tick)
        {
            // update bots according to logic
            _rlogic.tick(bangobj.getPieceArray(), tick);
            
            // count bots
            int rcount = 0;
            for (Piece piece : bangobj.pieces) {
                if (piece instanceof LoggingRobot && piece.isAlive()) {    
                    rcount++;
                }
            }
            
            // consider spawning another bot
            if (rcount < _rtarget &&
                RandomUtil.getInt(100) < 100 / AVG_ROBOT_SPAWN_DELAY) {
                spawnRobots(bangobj, 1);
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
                Unit unit = Unit.getUnit("indian_post/logging_robot");
                unit.assignPieceId(bangobj);
                unit.position(bspot.x, bspot.y);
                _bangmgr.addPiece(unit, (bangobj.tick >= 0) ?
                    AddPieceEffect.DROPPED : null);
            }
        }
        
        /** The logic used to control the robots. */
        protected LoggingRobotLogic _rlogic;
        
        /** The number of robots to keep alive. */
        protected int _rtarget;
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
         * and the amount of damage the target has already taken. */
        protected TargetEvaluator _teval = new TargetEvaluator() {
            public int getWeight (BangObject bangobj, Unit unit, Piece target, 
                    int dist, PointSet preferredMoves) {
                return unit.computeScaledDamage(bangobj, target, 1f) * 100 +
                    target.damage;
            }
        };
    }
    
    /** The spots from which robots emerge. */
    protected ArrayList<Marker> _robotSpots = new ArrayList<Marker>();
    
    /** The tree beds on the board. */
    protected ArrayList<TreeBed> _trees = new ArrayList<TreeBed>();
    
    /** The payouts for each player, determined at the end of the round. */
    protected int[] _payouts;
    
    /** The approximate number of logging robots to keep alive per unit. */
    protected static final float LOGGING_ROBOTS_PER_UNIT = 0.5f;
    
    /** The average number of ticks to allow before spawning a new robot. */
    protected static final int AVG_ROBOT_SPAWN_DELAY = 2;
    
    /** Payout adjustments for rankings in 2/3/4 player games. */
    protected static final int[][] PAYOUT_ADJUSTMENTS = {
        { -10, +10 }, { -10, 0, +10 }, { -10, -5, +5, +10 } };
}
