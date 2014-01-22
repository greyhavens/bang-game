//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.server.InvocationException;
import com.threerings.parlor.game.data.GameAI;
import com.threerings.stats.data.StatSet;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.BonusConfig;

import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.RandomLogic;

import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import com.threerings.bang.game.data.scenario.ScenarioInfo;

import static com.threerings.bang.Log.log;

/**
 * Implements a particular gameplay scenario.
 */
public abstract class Scenario
{
    /**
     * Called to initialize a scenario when it is created.
     */
    public void init (BangManager bangmgr, ScenarioInfo info)
    {
        _bangmgr = bangmgr;
        _info = info;

        // initialize our delegates
        for (ScenarioDelegate delegate : _delegates) {
            delegate.init(_bangmgr, this);
        }
    }

    /**
     * Returns the {@link ScenarioInfo} record associated with this scenario.
     */
    public ScenarioInfo getInfo ()
    {
        return _info;
    }

    /**
     * Allows a scenario to filter out custom marker pieces and scenario specific props and adjust
     * prop states prior to the start of the round.
     *
     * @param bangobj the game object.
     * @param starts an array of start markers for all the players.
     * @param pieces the remaining pieces on the board.
     * @param updates a list to populate with any pieces that were updated during the filter
     * process
     */
    public void filterPieces (BangObject bangobj, Piece[] starts, List<Piece> pieces,
                              List<Piece> updates)
    {
        // extract the bonus spawn markers from the pieces array
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (!p.isValidScenario(bangobj.scenario.getIdent())) {
                iter.remove();

            } else if (p instanceof Marker) {
                Marker m = (Marker)p;
                if (!bangobj.scenario.isValidMarker(m)) {
                    iter.remove();
                }
            }
        }

        // give our delegates a crack as well
        for (ScenarioDelegate delegate : _delegates) {
            delegate.filterPieces(bangobj, starts, pieces, updates);
        }
    }

    /**
     * Creates and returns an instance of {@link AILogic} to handle the behavior of the described
     * AI player.
     */
    public AILogic createAILogic (GameAI ai)
    {
        return new RandomLogic();
    }

    /**
     * Returns the maximum duration of this scenario in ticks.
     */
    public short getDuration (BangConfig bconfig, BangObject bangobj)
    {
        float duration = getBaseDuration() / (_bangmgr.getTeamSize()+1f);
        return (short)Math.ceil(duration * bconfig.duration.getAdjustment());
    }

    /**
     * Returns the number of milliseconds until the next tick.
     */
    public long getTickTime (BangConfig config, BangObject bangobj)
    {
        if (config.allPlayersAIs()) {
            // fast ticks for auto-play test games
            return 1000L * bangobj.getTotalUnitCount() / 4;
        }
        // start out with a base tick of two seconds and scale it down as the game progresses; cap
        // it at ten minutes
        long delta = System.currentTimeMillis() - _startStamp;
        delta = Math.min(delta, TIME_SCALE_CAP);

        // scale from 1/1 to 2/3 over the course of ten minutes
        float factor = 1f + getScaleFactor() * delta / TIME_SCALE_CAP;
        long baseTime = Math.round(BASE_TICK_TIME / factor);

        // scale this base time by the average number of units in play
        long tickTime = baseTime * getAverageUnitCount(bangobj);

        // make sure the tick is at least one second long
        return Math.max(tickTime, 1000L);
    }

    /**
     * Called when a round is about to start.
     *
     * @throws InvocationException containing a translatable string indicating why the scenario is
     * booched, which will be displayed to the players and the game will be cancelled.
     */
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
        throws InvocationException
    {
        // record the time at which the round started
        _startStamp = System.currentTimeMillis();

        // this will contain the starting spot for each player
        _startSpots = new Point[bangobj.players.length];
        for (int ii = 0; ii < _startSpots.length; ii++) {
            Piece p = starts[ii];
            _startSpots[ii] = new Point(p.x, p.y);
        }

        // allow our delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            delegate.roundWillStart(bangobj);
        }
    }

    /**
     * Called at the start of every game tick to allow the scenario to affect the game state and
     * determine whether or not the game should be ended.  If the scenario wishes to end the game
     * early, it should set {@link BangObject#lastTick} to the tick on which the game should end
     * (if it is set to the current tick the game will be ended when this call returns).
     *
     * @return true if any advance orders will need to be re-validated
     */
    public boolean tick (BangObject bangobj, short tick)
    {
        boolean validate = false;
        // allow our delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            try {
                validate = delegate.tick(bangobj, tick) || validate;
            } catch (Exception e) {
                log.warning("Delegate choked on tick", "game", _bangmgr.where(), "tick", tick,
                            "delegate", delegate, e);
            }
        }
        return validate;
    }

    /**
     * Called at the end of every tick by the game manager to potentially add a bonus to the board.
     *
     * @return true if a bonus was added, false if not.
     */
    public boolean addBonus (BangObject bangobj, List<Piece> pieces)
    {
        // if bonuses are disabled for this game, stop here
        if (bangobj.minCardBonusWeight >= 100) {
            return false;
        }

        // count up the unclaimed (non-scenario) bonuses on the board
        int bonuses = 0;
        for (Piece piece : pieces) {
            if (piece instanceof Bonus && !((Bonus)piece).isScenarioBonus()) {
                bonuses++;
            }
        }

        // have a 1 in 4 chance of adding a bonus for each live player for which there is not
        // already a bonus on the board excepting one
        int bprob = bangobj.gdata.livePlayers - bonuses - 1;
        int rando = RandomUtil.getInt(40);
        if (bprob < 0 || rando > bprob*10) {
            log.debug("No bonus, probability " + bprob + " in 10 (" + rando + ").");
            return false;
        }

        // enumerate the set of live pieces
        List<Unit> live = Lists.newArrayList();
        for (Piece piece : pieces) {
            if (piece.owner >= 0 && piece.isAlive() && piece instanceof Unit) {
                live.add((Unit)piece);
            }
        }

        // if there are no live units, abandon ship
        if (live.size() == 0) {
            return false;
        }

        // weight each of these pieces by the difference between the owning player's score and the
        // highest player's score
        int max = IntListUtil.getMaxValue(bangobj.points), candidates = 0;
        int[] weights = new int[live.size()];
        for (int ii = 0; ii < weights.length; ii++) {
            weights[ii] = max - bangobj.points[live.get(ii).owner];
            if (weights[ii] > 0) {
                candidates++;
            }
        }

        // now select a unit at (weighted) random
        Unit benefactor = (candidates > 0) ?
            live.get(RandomUtil.getWeightedIndex(weights)) : RandomUtil.pickRandom(live);
//         log.info("Placing bonus near " + benefactor);

        // compute this piece's valid moves
        PointSet moves = new PointSet();
        bangobj.board.computeMoves(benefactor, moves, null);

        // filter out spots that are not occupiable or contain a bonus
        for (int ii = 0; ii < moves.size(); ii++) {
            int x = moves.getX(ii), y = moves.getY(ii);
            if (!bangobj.board.isOccupiable(x, y)) {
                moves.remove(x, y);
                ii -= 1;
            }
        }
        if (moves.size() == 0) {
            return false;
        }

        // select a spot randomly from this piece's set of valid moves onto which to place the
        // bonus and call the standard algorithm
        int spidx = RandomUtil.getInt(moves.size());
        int x = moves.getX(spidx), y = moves.getY(spidx);
        PointSet spots = new PointSet();
        spots.add(x, y);

        // determine who can reach the selected spot and use that information to select and place a
        // bonus there
        ArrayIntSet[] reachers = computeReachers(bangobj, pieces, spots);
//         log.info("Placing bonus at +" + x + "+" + y +// "", "reachers", reachers[0]);
        placeBonus(bangobj, Bonus.selectBonus(bangobj, reachers[0]), x, y);
        return true;
    }

    /**
     * Called when a piece makes a move in the game. The scenario can deploy effects (via {@link
     * BangManager#deployEffect}) as a result of the move.
     */
    public void pieceMoved (BangObject bangobj, Piece piece)
    {
        // allow our delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            delegate.pieceMoved(bangobj, piece);
        }
    }

    /**
     * Called when a piece was killed. The scenario can choose to respawn the piece later, and do
     * whatever else is appropriate.
     */
    public void pieceWasKilled (BangObject bangobj, Piece piece, int shooter, int sidx)
    {
        // allow our delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            delegate.pieceWasKilled(bangobj, piece, shooter, sidx);
        }
    }

    /**
     * Called when a piece was removed.
     */
    public void pieceWasRemoved (BangObject bangobj, Piece piece)
    {
        // allow our delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            delegate.pieceWasRemoved(bangobj, piece);
        }
    }

    /**
     * Called when a piece is affected.
     */
    public void pieceAffected (Piece piece, String effect)
    {
        // allow out delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            delegate.pieceAffected(piece, effect);
        }
    }

    /**
     * Called when a round has ended, giving the scenario a chance to award any final cash and
     * increment associated statistics.
     */
    public void roundDidEnd (BangObject bangobj)
    {
        // allow our delegates to participate
        for (ScenarioDelegate delegate : _delegates) {
            delegate.roundDidEnd(bangobj);
        }
    }

    /**
     * Modifies the damage done by the first player to the second for purposes of modifying
     * statistics and awarding bonus points.
     */
    public int modifyDamageDone (int pidx, int tidx, int ddone)
    {
        return ddone;
    }

    /**
     * Returns true if this scenario type should pay out earnings to the specified player. False if
     * not.
     */
    public boolean shouldPayEarnings (PlayerObject user)
    {
        return true;
    }

    /**
     * Computes the per-round earnings for the given player.
     */
    public int computeEarnings (BangObject bangobj, int pidx, int ridx,
                                BangManager.PlayerRecord[] precords, BangManager.RankRecord[] ranks,
                                BangManager.RoundRecord[] rounds)
    {
        // scale their earnings by the number of players they defeated in each round
        int defeated = 0, aisDefeated = 0;
        boolean team = bangobj.isTeamGame();
        for (int ii = ranks.length-1; ii >= 0; ii--) {
            // stop when we get to our record
            if (team && bangobj.teams[ranks[ii].pidx] == bangobj.teams[pidx]) {
                if (defeated - aisDefeated >= 2) {
                    defeated++;
                }
                break;
            } else if (!team && ranks[ii].pidx == pidx) {
                break;
            }

            // require that the opponent finished at least half the round
            if (precords[ranks[ii].pidx].finishedTick[ridx] <
                rounds[ridx].duration/2) {
                continue;
            }

            if (_bangmgr.isAI(ranks[ii].pidx)) {
                // only the first AI counts toward earnings
                if (++aisDefeated <= 1) {
                    defeated++;
                }
            } else {
                defeated++;
            }
        }

        log.debug("Noting earnings p:" + bangobj.players[pidx] + " r:" + ridx +
                  " (" + precords[pidx].finishedTick[ridx] + " * " +
                  BASE_EARNINGS[defeated] + " / " + rounds[ridx].lastTick + ").");

        // scale the player's earnings based on the percentage of the round they completed
        return (precords[pidx].finishedTick[ridx] * BASE_EARNINGS[defeated] /
                (rounds[ridx].duration - 1));
    }

    /**
     * Gives the scenario an opportunity to record statistics for the supplied player at the end of
     * the game.
     */
    public void recordStats (
        StatSet[] stats, int gameTime, int pidx, PlayerObject user)
    {
        // nothing by default
    }

    /**
     * Registers a delegate for this scenario. This should be called by derived classes
     * <em>before</em> {@link #init} is called. Generally this means they should register their
     * delegates in their constructor.
     */
    protected void registerDelegate (ScenarioDelegate delegate)
    {
        _delegates.add(delegate);
    }

    /**
     * Returns the respawn location for the specified player index.
     */
    protected Point getStartSpot (int pidx)
    {
        return _startSpots[pidx];
    }

    /**
     * Locates a place for the specified bonus, based on the current unit location, ranking and
     * other metadata of the various players.
     *
     * @return true if the bonus was placed, false if a spot could not be located.
     */
    protected boolean placeBonus (BangObject bangobj, List<Piece> pieces,
                                  Bonus bonus, PointSet spots)
    {
        // determine (roughly) who can get to bonus spots on this tick
        ArrayIntSet[] reachers = computeReachers(bangobj, pieces, spots);

        // now convert reachability into weightings for each of the spots
        int[] weights = new int[spots.size()];
        for (int ii = 0; ii < weights.length; ii++) {
            if (reachers[ii] == null) {
//                 log.info("Spot " + ii + " is a wash.");
                // if no one can reach it, give it a base probability
                weights[ii] = 1;

            } else if (reachers[ii].size() == 1) {
//                 log.info("Spot " + ii + " is a one man spot.");
                // if only one player can reach it, give it a probability inversely proportional to
                // that player's power
                int pidx = reachers[ii].get(0);
                float ifactor = 1f - bangobj.pdata[pidx].powerFactor;
                weights[ii] = Math.round(10 * Math.max(0, ifactor)) + 1;

            } else {
                // if multiple players can reach it, give it a nudge if they are of about equal
                // power
                float avgpow = bangobj.getAveragePower(reachers[ii]);
                boolean outlier = false;
                for (int pp = 0; pp < reachers[ii].size(); pp++) {
                    int pidx = reachers[ii].get(pp);
                    float power = bangobj.pdata[pidx].power;
                    if (power < 0.9 * avgpow || power > 1.1 * avgpow) {
                        outlier = true;
                    }
                }
//                 log.info("Spot " + ii + " is a multi-man spot: " + outlier);
                weights[ii] = outlier ? 1 : 5;
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
        log.debug("Selecting from " + StringUtil.toString(weights) + ": " + spidx + " -> " +
                  spot.x + "/" + spot.y + ".");

        // locate the nearest spot to that which can be occupied by our piece
        Point bspot = bangobj.board.getOccupiableSpot(spot.x, spot.y, 3);
        if (bspot == null) {
            log.info("Dropping bonus for lack of spot " + spot + ".");
            return false;
        }

        // if no specific bonus was requested, pick one randomly based on who can reach it
        if (bonus == null) {
            bonus = Bonus.selectBonus(bangobj, reachers[spidx]);
        }

        placeBonus(bangobj, bonus, bspot.x, bspot.y);
        return true;
    }

    /**
     * Places a bonus at the specified spot on the board.
     */
    protected void placeBonus (BangObject bangobj, Bonus bonus, int x, int y)
    {
        // configure our bonus and add it to the game
        bonus.assignPieceId(bangobj);
        bonus.position(x, y);
        _bangmgr.addPiece(bonus);
        log.debug("Placed bonus: " + bonus);
    }

    /**
     * Drops a bonus as close to the specified location that it can find.
     */
    protected Bonus dropBonus (
        BangObject bangobj, String bonusName, int x, int y)
    {
        Bonus drop = Bonus.createBonus(BonusConfig.getConfig(bonusName));
        Point spot = bangobj.board.getOccupiableSpot(x, y, 0, 3, null);
        if (spot == null) {
            log.info("Unable to drop bonus for lack of spot", "x", x, "y", y);
            return null;
        }
        drop.assignPieceId(bangobj);
        drop.position(spot.x, spot.y);
        _bangmgr.addPiece(drop);
        return drop;
    }

    /**
     * Computes the set of all pieces that are ready to be moved and can reach the specified set of
     * positions on the board. If no pieces can reach a spot, no set will be created, the array
     * element will be null.
     */
    protected ArrayIntSet[] computeReachers (BangObject bangobj, List<Piece> pieces, PointSet spots)
    {
        ArrayIntSet[] reachers = new ArrayIntSet[spots.size()];
        for (Piece p : pieces) {
            if (!(p instanceof Unit) || p.owner < 0 || !p.isAlive() ||
                p.ticksUntilMovable(bangobj.tick) > 0) {
                continue;
            }

            // compute this piece's move set
            _tpoints.clear();
            bangobj.board.computeMoves(p, _tpoints, null);

            // determine which of the spots this piece can reach
            for (int bb = 0; bb < reachers.length; bb++) {
                int x = spots.getX(bb), y = spots.getY(bb);
                if (!_tpoints.contains(x, y)) {
                    continue;
                }
//                 log.info(p + " can reach " + x + "/" + y);
                if (reachers[bb] == null) {
                    reachers[bb] = new ArrayIntSet();
                    reachers[bb].add(p.owner);
                }
            }
        }
        return reachers;
    }

    /**
     * Returns the average number of live units per player.
     */
    protected int getAverageUnitCount (BangObject bangobj)
    {
        return bangobj.getAverageUnitCount();
    }

    /**
     * Returns the base duration of the scenario in ticks assuming 1.75 seconds per tick. This will
     * be scaled by the expected average number of units per player to obtain a real duration.
     */
    protected short getBaseDuration ()
    {
        return BASE_SCENARIO_TICKS;
    }

    /**
     * Returns the scale factor for ticks as the game progresses.
     */
    protected float getScaleFactor ()
    {
        return 0.5f;
    }

    /**
     * Helper function useful when initializing scenarios. Determines the player whose start marker
     * is closest to the specified piece and is therefore the <em>owner</em> of that piece.
     *
     * @param exclude a set of player indices whose start markers should be excluded from the
     * search
     * @return -1 if no start markers exist at all or the player index of the closest marker.
     */
    protected int getOwner (Piece target, ArrayIntSet exclude)
    {
        int mindist = Integer.MAX_VALUE, idx = -1;
        for (int ii = 0; ii < _startSpots.length; ii++) {
            if (exclude.contains(ii)) {
                continue;
            }
            int dist = target.getDistance(_startSpots[ii].x, _startSpots[ii].y);
            if (dist < mindist) {
                mindist = dist;
                idx = ii;
            }
        }
        return idx;
    }

    /** The Bang game manager. */
    protected BangManager _bangmgr;

    /** The game configuration. */
    protected BangConfig _config;

    /** Our metadata. */
    protected ScenarioInfo _info;

    /** Delegates that handle certain aspects of our scenario. */
    protected List<ScenarioDelegate> _delegates = Lists.newArrayList();

    /** Used to track the locations where players are started. */
    protected Point[] _startSpots;

    /** Used when determining where to place a bonus. */
    protected PointSet _tpoints = new PointSet();

    /** The time at which the round started. */
    protected long _startStamp;

    /** The default base tick time. */
    protected static final long BASE_TICK_TIME = 2000L;

    /** We stop reducing the tick time after ten minutes. */
    protected static final long TIME_SCALE_CAP = 10 * 60 * 1000L;

    /** The base number of ticks that we allow per round (scaled by the anticipated average number
     * of units per-player). */
    protected static final short BASE_SCENARIO_TICKS = 300;

    /** A set of times (in seconds prior to the end of the round) at which we warn the players. */
    protected static final long[] TIME_WARNINGS = { 60*1000L, 30*1000L, 10*1000L };

    /** Defines the base earnings (per-round) for each rank. */
    protected static final int[] BASE_EARNINGS = { 100, 140, 170, 210 };
}
