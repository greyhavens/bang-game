//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;
import com.threerings.media.util.MathUtil;

import com.threerings.presents.server.InvocationException;
import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.BonusConfig;

import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.RandomLogic;

import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Implements a particular gameplay scenario.
 */
public abstract class Scenario
{
    /**
     * Drops a bonus at the specified location.
     */
    protected static Bonus dropBonus (
            BangObject bangobj, String bonusName, int x, int y)
    {
        Bonus drop = Bonus.createBonus(BonusConfig.getConfig(bonusName));
        drop.assignPieceId(bangobj);
        drop.position(x, y);
        bangobj.board.shadowPiece(drop);
        bangobj.addToPieces(drop);
        return drop;
    }
    
    /**
     * Allows a scenario to filter out custom marker pieces and scenario
     * specific props prior to the start of the round.
     * <em>Note:</em> this is called before {@link #init}.
     *
     * @param bangobj the game object.
     * @param starts a list of start markers for all the players.
     * @param pieces the remaining pieces on the board.
     */
    public void filterPieces (BangObject bangobj, ArrayList<Piece> starts,
                              ArrayList<Piece> pieces)
    {
        // extract the bonus spawn markers from the pieces array
        _bonusSpots.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (!p.isValidScenario(bangobj.scenarioId)) {
                iter.remove();

            } else if (Marker.isMarker(p, Marker.BONUS)) {
                _bonusSpots.add(p.x, p.y);
                iter.remove();
            }
        }
    }

    /**
     * Called to initialize a scenario when it is created.
     */
    public void init (BangManager bangmgr)
    {
        _bangmgr = bangmgr;

        // initialize our delegates
        for (ScenarioDelegate delegate : _delegates) {
            delegate.init(_bangmgr, this);
        }
    }

    /**
     * Creates and returns an instance of {@link AILogic} to handle the
     * behavior of the described AI player.
     */
    public AILogic createAILogic (GameAI ai)
    {
        return new RandomLogic();
    }

    /**
     * Determines the next phase of the game. Normally a game transitions from
     * {@link BangObject#SELECT_PHASE} to {@link BangObject#BUYING_PHASE} to
     * {@link BangObject#IN_PLAY}, but the tutorial scenario skips some of
     * those phases.
     */
    public void startNextPhase (BangObject bangobj)
    {
        switch (bangobj.state) {
        case BangObject.POST_ROUND:
        case BangObject.PRE_GAME:
            _bangmgr.startPhase(BangObject.SELECT_PHASE);
            break;

        case BangObject.SELECT_PHASE:
            _bangmgr.startPhase(BangObject.BUYING_PHASE);
            break;

        case BangObject.BUYING_PHASE:
            _bangmgr.startPhase(BangObject.IN_PLAY);
            break;

        default:
            log.warning("Unable to start next phase [game=" + bangobj.which() +
                        ", state=" + bangobj.state + "].");
            break;
        }
    }

    /**
     * Returns the maximum duration of this scenario in ticks.
     */
    public short getDuration (BangConfig bconfig)
    {
        return (short)Math.ceil(getBaseDuration() / (bconfig.teamSize+1f));
    }

    /**
     * Called when a round is about to start.
     *
     * @throws InvocationException containing a translatable string
     * indicating why the scenario is booched, which will be displayed to
     * the players and the game will be cancelled.
     */
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> starts,
                                PieceSet purchases)
        throws InvocationException
    {
        // this will contain the starting spot for each player
        _startSpots = new Point[bangobj.players.length];
        for (int ii = 0; ii < _startSpots.length; ii++) {
            Piece p = starts.get(ii);
            _startSpots[ii] = new Point(p.x, p.y);
        }

        // allow our delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            delegate.roundWillStart(bangobj);
        }
    }

    /**
     * Called at the start of every game tick to allow the scenario to affect
     * the game state and determine whether or not the game should be ended.
     * If the scenario wishes to end the game early, it should set {@link
     * BangObject#lastTick} to the tick on which the game should end (if it is
     * set to the current tick the game will be ended when this call returns).
     */
    public void tick (BangObject bangobj, short tick)
    {
        // allow our delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            delegate.tick(bangobj, tick);
        }
    }

    /**
     * Called at the end of every tick by the game manager to potentially add a
     * bonus to the board.
     *
     * @return true if a bonus was added, false if not.
     */
    public boolean addBonus (BangObject bangobj, Piece[] pieces)
    {
        // have a 1 in 4 chance of adding a bonus for each live player for
        // which there is not already a bonus on the board
        int bprob = (bangobj.gdata.livePlayers - bangobj.gdata.bonuses);
        int rando = RandomUtil.getInt(40);
        if (bprob == 0 || rando > bprob*10) {
            log.fine("No bonus, probability " + bprob + " in 10 (" +
                     rando + ").");
            return false;
        }
        return placeBonus(bangobj, pieces, null, _bonusSpots);
    }

    /**
     * Called when a piece makes a move in the game. The scenario can deploy
     * effects (via {@link BangManager#deployEffect}) as a result of the move.
     */
    public void pieceMoved (BangObject bangobj, Piece piece)
    {
        // allow our delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            delegate.pieceMoved(bangobj, piece);
        }
    }

    /**
     * Called when a piece was killed. The scenario can choose to respawn the
     * piece later, and do whatever else is appropriate.
     */
    public void pieceWasKilled (BangObject bangobj, Piece piece)
    {
        // allow our delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            delegate.pieceWasKilled(bangobj, piece);
        }
    }

    /**
     * Called when a round has ended, giving the scenario a chance to award any
     * final cash and increment associated statistics.
     */
    public void roundDidEnd (BangObject bangobj)
    {
        // allow our delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            delegate.roundDidEnd(bangobj);
        }
    }

    /**
     * Returns true if this scenario type should pay out earnings to the
     * specified player. False if not.
     */
    public boolean shouldPayEarnings (PlayerObject user)
    {
        return true;
    }

    /**
     * Gives the scenario an opportunity to record statistics for the supplied
     * player at the end of the game.
     */
    public void recordStats (
        BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        // nothing by default
    }

    /**
     * Registers a delegate for this scenario. This should be called by derived
     * classes <em>before</em> {@link #init} is called. Generally this means
     * they should register their delegates in their constructor.
     */
    protected void registerDelegate (ScenarioDelegate delegate)
    {
        _delegates.add(delegate);
    }

    /**
     * Locates a place for the specified bonus, based on the current ranking of
     * the various players.
     *
     * @return true if the bonus was placed, false if a spot could not be
     * located.
     */
    protected boolean placeBonus (BangObject bangobj, Piece[] pieces,
                                  Bonus bonus, PointSet spots)
    {
        // determine (roughly) who can get to bonus spots on this tick
        int[] weights = new int[spots.size()];
        ArrayIntSet[] reachers = new ArrayIntSet[weights.length];
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (!(p instanceof Unit) || p.owner < 0 || !p.isAlive() ||
                p.ticksUntilMovable(bangobj.tick) > 0) {
                continue;
            }
            for (int bb = 0; bb < reachers.length; bb++) {
                int x = spots.getX(bb), y = spots.getY(bb);
                if (p.getDistance(x, y) > p.getMoveDistance()) {
                    continue;
                }
                _tpoints.clear();
                bangobj.board.computeMoves(p, _tpoints, null);
                if (!_tpoints.contains(x, y)) {
                    continue;
                }
//                 log.info(p.info() + " can reach " + x + "/" + y);
                if (reachers[bb] == null) {
                    reachers[bb] = new ArrayIntSet();
                    reachers[bb].add(p.owner);
                }
            }
        }

        // now convert reachability into weightings for each of the spots
        for (int ii = 0; ii < weights.length; ii++) {
            if (reachers[ii] == null) {
//                 log.info("Spot " + ii + " is a wash.");
                // if no one can reach it, give it a base probability
                weights[ii] = 1;

            } else if (reachers[ii].size() == 1) {
//                 log.info("Spot " + ii + " is a one man spot.");
                // if only one player can reach it, give it a probability
                // inversely proportional to that player's power
                int pidx = reachers[ii].get(0);
                double ifactor = 1.0 - bangobj.pdata[pidx].powerFactor;
                weights[ii] = (int)Math.round(10 * Math.max(0, ifactor)) + 1;

            } else {
                // if multiple players can reach it, give it a nudge if
                // they are of about equal power
                double avgpow = bangobj.getAveragePower(reachers[ii]);
                boolean outlier = false;
                for (int pp = 0; pp < reachers[ii].size(); pp++) {
                    int pidx = reachers[ii].get(pp);
                    double power = bangobj.pdata[pidx].power;
                    if (power < 0.9 * avgpow || power > 1.1 * avgpow) {
                        outlier = true;
                    }
                }
//                 log.info("Spot " + ii + " is a multi-man spot: " + outlier);
                weights[ii] = outlier ? 1 : 5;
            }
        }

        // if we're placing this at oen of the standard bonus spots, zero out
        // weightings for any spots that already have a bonus
        if (spots == _bonusSpots) {
            for (int ii = 0; ii < pieces.length; ii++) {
                if (pieces[ii] instanceof Bonus) {
                    int spidx = ((Bonus)pieces[ii]).spot;
                    if (spidx >= 0) {
                        weights[spidx] = 0;
                    }
                }
            }
        }

        // make sure there is at least one available spot
        if (IntListUtil.sum(weights) == 0) {
            log.info("Dropping bonus. No unused spots.");
            return false;
        }

        // now select a spot based on our weightings
        int spidx = RandomUtil.getWeightedIndex(weights);
        Point spot = new Point(spots.getX(spidx), spots.getY(spidx));
        log.fine("Selecting from " + StringUtil.toString(weights) + ": " +
                 spidx + " -> " + spot.x + "/" + spot.y + ".");

        // locate the nearest spot to that which can be occupied by our piece
        Point bspot = bangobj.board.getOccupiableSpot(spot.x, spot.y, 3);
        if (bspot == null) {
            log.info("Dropping bonus for lack of spot " + spot + ".");
            return false;
        }

        // if no specific bonus was requested, pick one randomly based on who
        // can reach it
        if (bonus == null) {
            bonus = Bonus.selectBonus(bangobj, bspot, reachers[spidx]);
        }

        // if we're placing this at one of the standard bonus spots, note that
        // that spot is "occupied" until this bonus is colleted
        if (spots == _bonusSpots) {
            bonus.spot = (short)spidx;
        }

        // configure our bonus and add it to the game
        bonus.assignPieceId(bangobj);
        bonus.position(bspot.x, bspot.y);
        bangobj.addToPieces(bonus);
        bangobj.board.shadowPiece(bonus);
        log.fine("Placed bonus: " + bonus.info());
        return true;
    }

    /**
     * Returns the base duration of the scenario in ticks assuming 1.75 seconds
     * per tick. This will be scaled by the expected average number of units
     * per player to obtain a real duration.
     */
    protected short getBaseDuration ()
    {
        return BASE_SCENARIO_TICKS;
    }

    /**
     * Helper function useful when initializing scenarios. Determines the
     * player whose start marker is closest to the specified piece and is
     * therefore the <em>owner</em> of that piece.
     *
     * @return -1 if no start markers exist at all or the player index of the
     * closest marker.
     */
    protected int getOwner (Piece target)
    {
        int mindist2 = Integer.MAX_VALUE, idx = -1;
        for (int ii = 0; ii < _startSpots.length; ii++) {
            int dist2 = MathUtil.distanceSq(
                target.x, target.y, _startSpots[ii].x, _startSpots[ii].y);
            if (dist2 < mindist2) {
                mindist2 = dist2;
                idx = ii;
            }
        }
        return idx;
    }

    /**
     * Returns a list of bonus spot indexes in order descending order based on
     * distance to starting positions.
     */
    protected ArrayList<BonusSorter> sortBonusList ()
    {
        // sort the bonus spots by distance to nearest starting point
        ArrayList<BonusSorter> sorters = new ArrayList<BonusSorter>();
        for (int ii = 0; ii < _bonusSpots.size(); ii++) {
            BonusSorter sorter = new BonusSorter();
            sorter.index = (short)ii;
            int x = _bonusSpots.getX(ii), y = _bonusSpots.getY(ii);
            for (int ss = 0; ss < _startSpots.length; ss++) {
                int distsq = MathUtil.distanceSq(
                    x, y, _startSpots[ss].x, _startSpots[ss].y);
                sorter.minDistSq = Math.max(sorter.minDistSq, distsq);
            }
            sorters.add(sorter);
        }
        Collections.sort(sorters);
        return sorters;
    }

    protected static class BonusSorter implements Comparable<BonusSorter>
    {
        /** The index of this bonus spot. */
        public short index;

        /** The distance (squared) from the bonus spot to the closest player. */        public int minDistSq;

        /** Compare based on distance to nearest player. */
        public int compareTo (BonusSorter other) {
            return other.minDistSq - minDistSq;
        }
    }

    /** The Bang game manager. */
    protected BangManager _bangmgr;

    /** Delegates that handle certain aspects of our scenario. */
    protected ArrayList<ScenarioDelegate> _delegates =
        new ArrayList<ScenarioDelegate>();

    /** Used to track the locations where players are started. */
    protected Point[] _startSpots;

    /** Used to track the locations of all bonus spawn points. */
    protected PointSet _bonusSpots = new PointSet();

    /** Used when determining where to place a bonus. */
    protected PointSet _tpoints = new PointSet();

    /** The base number of ticks that we allow per round (scaled by the
     * anticipated average number of units per-player). */
    protected static final short BASE_SCENARIO_TICKS = 300;

    /** A set of times (in seconds prior to the end of the round) at which
     * we warn the players. */
    protected static final long[] TIME_WARNINGS = {
        60*1000L, 30*1000L, 10*1000L };
}
