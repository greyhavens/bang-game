//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.samskivert.util.RandomUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.LoggingRobot;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TreeBed;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li> Trees sprout from fixed locations on the board.
 * <li> Players surround the trees to make them grow.
 * <li> Logging robots attempt to cut down the trees.
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
    public void filterPieces (BangObject bangobj, ArrayList<Piece> starts,
                              ArrayList<Piece> pieces)
    {
        super.filterPieces(bangobj, starts, pieces);

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
    public int modifyDamageDone (int pidx, int tidx, int ddone)
    {
        // no points are awarded for shooting the other players' units; only
        // for shooting the robots
        return (tidx == -1) ? ddone : 0;
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

        // note trees grown to full height
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof TreeBed &&
                ((TreeBed)piece).growth == TreeBed.FULLY_GROWN) {
                for (StatSet stats : bangobj.stats) {
                    stats.incrementStat(Stat.Type.TREES_GROWN, 1);
                }
            }
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
    
    /** Controls the logging robots. */
    protected class LoggingRobotDelegate extends ScenarioDelegate
    {
        @Override // documentation inherited
        public void roundWillStart (BangObject bangobj)
        {
            _rlogic = new LoggingRobotLogic();
            _rlogic.init(_bangmgr, -1);
        }
        
        @Override // documentation inherited
        public void tick (BangObject bangobj, short tick)
        {
            // update bots according to logic
            _rlogic.tick(bangobj.getPieceArray(), tick);
            
            // count trees and bots
            int tcount = 0, bcount = 0;
            for (Piece piece : bangobj.pieces) {
                if (piece instanceof TreeBed && ((TreeBed)piece).growth > 0) {
                    tcount++;
                } else if (piece instanceof LoggingRobot && piece.isAlive()) {
                    bcount++;
                }
            }
            
            // consider spawning another bot
            if (bcount < tcount * LOGGING_ROBOTS_PER_TREE &&
                RandomUtil.getInt(100) < 100 / AVG_ROBOT_SPAWN_DELAY) {
                spawnRobot(bangobj);
            }
        }
        
        protected void spawnRobot (BangObject bangobj)
        {
            Unit unit = Unit.getUnit("indian_post/logging_robot");
            unit.assignPieceId(bangobj);
            
            Point bspot = null;
            Collections.shuffle(_robotSpots);
            for (Marker marker : _robotSpots) {
                if ((bspot = bangobj.board.getOccupiableSpot(
                        marker.x, marker.y, 3)) != null) {
                    break;
                }
            }
            if (bspot == null) {
                log.warning("Unable to locate spawn spot for logging robot " +
                    "[spots=" + _robotSpots + "].");
                return;
            }
            
            unit.position(bspot.x, bspot.y);
            bangobj.addToPieces(unit);
            bangobj.board.shadowPiece(unit);
        }
        
        /** The logic used to control the robots. */
        protected LoggingRobotLogic _rlogic;
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
                    if ((tree.growth > 0 || tree.damage < 100) &&
                        (ctree == null || unit.getDistance(tree) <
                            unit.getDistance(ctree))) {
                        ctree = tree;
                    }
                } else if (piece instanceof Unit &&
                    unit.validTarget(piece, false) && (cunit == null ||
                        unit.getDistance(piece) < unit.getDistance(cunit))) {
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
            public int getWeight (
                    BangObject bangobj, Unit unit, Piece target, int dist) {
                return unit.computeScaledDamage(bangobj, target, 1f) * 100 +
                    target.damage;
            }
        };
    }
    
    /** The spots from which robots emerge. */
    protected ArrayList<Marker> _robotSpots = new ArrayList<Marker>();
    
    /** The approximate number of logging robots to keep alive per tree. */
    protected static final int LOGGING_ROBOTS_PER_TREE = 1;
    
    /** The average number of ticks to allow before spawning a new robot. */
    protected static final int AVG_ROBOT_SPAWN_DELAY = 4;
}
