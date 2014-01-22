//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.server.InvocationException;
import com.threerings.parlor.game.data.GameAI;
import com.threerings.stats.data.StatSet;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Rating;
import com.threerings.bang.data.StatType;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.RobotWaveEffect;
import com.threerings.bang.game.data.piece.Bonus;
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
        registerDelegate(_respdel = new RespawnDelegate(8));
        registerDelegate(_logrobdel = new LoggingRobotDelegate());
        registerDelegate(_treedel = new TreeBedDelegate());
    }

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new ForestGuardiansLogic();
    }

    @Override // documentation inherited
    public int modifyDamageDone (int pidx, int tidx, int ddone)
    {
        // points are granted for damaging the robots; subtracted for damaging
        // your teammates
        return (tidx == -1) ? ddone : (-3 * ddone / 2);
    }

    @Override // documentation inherited
    public void filterPieces (BangObject bangobj, Piece[] starts, List<Piece> pieces,
                              List<Piece> updates)
    {
        super.filterPieces(bangobj, starts, pieces, updates);

        // extract and remove all fetish markers
        _fetishSpots.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.FETISH)) {
                _fetishSpots.add(p.x, p.y);
                iter.remove();
            }
        }
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // set the initial difficulty level based on the average player rating
        int nplayers = _bangmgr.getPlayerSlots(), trating = 0;
        for (int ii = 0; ii < nplayers; ii++) {
            trating += _bangmgr.getPlayerRecord(ii).getRating(
                ForestGuardiansInfo.IDENT, null).rating;
        }
        float rratio = ((float)(trating / nplayers) - Rating.DEFAULT_RATING) /
            (Rating.MAXIMUM_RATING - Rating.DEFAULT_RATING);
        _difficulty = Math.max(0, Math.round(rratio * MAX_INITIAL_DIFFICULTY));

        String sdata = _bangmgr.getBangConfig().getScenarioData(bangobj.roundId);
        if (!StringUtil.isBlank(sdata)) {
            try {
                int diff = Integer.parseInt(sdata);
                if (diff >= 0 && diff < MAX_DIFFICULTY) {
                    _difficulty = diff;
                }
            } catch (Exception e) {
                // nothing doing
            }
        }

        // add a random subset of the tree beds
        _treedel.resetTrees(bangobj, _difficulty);

        // place the four fetishes
        List<Piece> pieces = bangobj.getPieceArray();
        placeBonus(bangobj, pieces,
            Bonus.createBonus("indian_post/fetish_bear"), _fetishSpots);
        placeBonus(bangobj, pieces,
            Bonus.createBonus("indian_post/fetish_fox"), _fetishSpots);
        placeBonus(bangobj, pieces,
            Bonus.createBonus("indian_post/fetish_frog"), _fetishSpots);
        placeBonus(bangobj, pieces,
            Bonus.createBonus("indian_post/fetish_turtle"), _fetishSpots);

        // start the first wave on the second tick
        _nextWaveTick = NEXT_WAVE_TICKS;
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        boolean validate = super.tick(bangobj, tick);

        if (tick < _nextWaveTick) {
            // don't do anything until the wave has started
            return validate;

        } else if (tick == _nextWaveTick) {
            startNextWave(bangobj);

        } else {
            // if all trees are grown and all bots are dead, end the wave
            switch (_logrobdel.isWaveOver(bangobj, tick)) {
            case END_WAVE:
                endWave(bangobj, tick);
                break;
            case END_GAME:
                bangobj.setLastTick(tick);
                break;
            default:
                break; // nada
            }
        }

        return validate;
    }

    @Override // documentation inherited
    public void roundDidEnd (BangObject bangobj)
    {
        super.roundDidEnd(bangobj);

        // nothing to do if we end before the first wave
        if (_wave < 1) {
            return;
        }

        // have the tree bed delegate compute our final score and payouts
        int treePoints = _treesGrown * TreeBed.FULLY_GROWN;
        int maxPoints = _waveMax * TreeBed.FULLY_GROWN;
        // int living = 0, total = 0;
        for (TreeBed tree : _treedel.getWaveTrees()) {
            if (tree.growth == 0) {
                continue;
            }
            // total++;
            maxPoints += tree.growth;
            if (!tree.isAlive()) {
                continue;
            }
            // living++;
            treePoints += tree.growth;
            int points = scalePoints(
                ForestGuardiansInfo.GROWTH_POINTS[tree.growth-1]);
            for (int ii = 0; ii < bangobj.stats.length; ii++) {
                bangobj.stats[ii].incrementStat(
                    ForestGuardiansInfo.GROWTH_STATS[tree.growth-1], 1);
                bangobj.stats[ii].incrementStat(StatType.WAVE_POINTS, points);
                bangobj.grantPoints(ii, points);
            }
        }

        // TODO: do we want to report a performance for the last wave? even if they grew no trees?
            // int perf = (total > 0 ? RobotWaveEffect.getPerformance(living, total) :
            //             RobotWaveEffect.MAX_PERFORMANCE);
            // for (StatSet stats : bangobj.stats) {
            //     stats.appendStat(StatType.WAVE_SCORES, perf);
            // }

        _payouts = new int[bangobj.players.length];
        Arrays.fill(_payouts,
                (maxPoints > 0 ? _waveBonus + 60 + (95 - 60) * treePoints / maxPoints : 0));

        int[] points = bangobj.perRoundPoints[bangobj.roundId];
        int[] spoints = new ArrayIntSet(points).toIntArray();
        if (spoints.length >= 2) {
            for (int ii = 0; ii < _payouts.length; ii++) {
                int ppoints = points[ii];
                int pcount = count(points, ppoints);
                if (pcount > 0) {
                    _payouts[ii] += PAYOUT_ADJUSTMENTS[spoints.length-2][
                        IntListUtil.indexOf(spoints, ppoints)] / pcount;
                }
            }
        }
    }

    @Override // documentation inherited
    public void recordStats (
        StatSet[] stats, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(stats, gameTime, pidx, user);

        // record the number of trees they grew
        for (int ii = 0; ii < ForestGuardiansInfo.GROWTH_STATS.length; ii++) {
            StatType stat = ForestGuardiansInfo.GROWTH_STATS[ii];
            user.stats.incrementStat(stat, stats[pidx].getIntStat(stat));
        }

        // record other accumulating stats
        for (int ii = 0; ii < ACCUM_STATS.length; ii++) {
            user.stats.incrementStat(
                ACCUM_STATS[ii], stats[pidx].getIntStat(ACCUM_STATS[ii]));
        }

        // and the highest difficulty level reached
        user.stats.maxStat(StatType.HIGHEST_SAWS, _difficulty + 1);
    }

    @Override // documentation inherited
    public int computeEarnings (
        BangObject bangobj, int pidx, int ridx,
        BangManager.PlayerRecord[] precords, BangManager.RankRecord[] ranks,
        BangManager.RoundRecord[] rounds)
    {
        if (_payouts == null) {
            return 0;
        }

        // if the game ended too early, scale the earnings
        int duration = rounds[ridx].lastTick;
        if (rounds[ridx].duration - rounds[ridx].lastTick > MIN_WAVE_TICKS) {
            duration = rounds[ridx].duration;
        }
        return (precords[pidx].finishedTick[ridx] * _payouts[pidx] / duration);
    }

    @Override // documentation inherited
    protected int getAverageUnitCount (BangObject bangobj)
    {
        // we want to include the computer controlled units in this calculation
        int units = 0, players = 0;
        boolean[] liveUnits = new boolean[bangobj.players.length + 1];
        for (Piece p : bangobj.pieces) {
            if (p instanceof Unit && p.isAlive()) {
                units++;
                if (!liveUnits[p.owner + 1]) {
                    players++;
                    liveUnits[p.owner + 1] = true;
                }
            }
        }
        players = Math.max(1, players);
        return Math.round(units / players);
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
     * Ends the current wave and either starts the next or ends the game.
     */
    protected void endWave (BangObject bangobj, short tick)
    {
        // clear all advance orders
        _bangmgr.clearOrders();

        // announce the end of the wave
        RobotWaveEffect rweffect = new RobotWaveEffect(_wave);
        _bangmgr.deployEffect(-1, rweffect);
        int grown = rweffect.living, perf = rweffect.getPerformance();
        _treesGrown += grown;
        _waveMax += _treedel.getWaveTrees().size();

        // record stats and points for trees grown, add the score to the total
        int points = scalePoints(
            ForestGuardiansInfo.GROWTH_POINTS[TreeBed.FULLY_GROWN - 1] * grown);
        for (int ii = 0; ii < bangobj.stats.length; ii++) {
            bangobj.stats[ii].incrementStat(StatType.TREES_ELDER, grown);
            bangobj.stats[ii].appendStat(StatType.WAVE_SCORES, perf);
            if (perf == RobotWaveEffect.MAX_PERFORMANCE) {
                bangobj.stats[ii].incrementStat(StatType.PERFECT_WAVES, 1);
            }
            bangobj.stats[ii].incrementStat(StatType.WAVE_POINTS, points);
            bangobj.grantPoints(ii, points);
        }

        // give out a wave bonus for successful completion of a wave
        if (grown >= _treedel.getWaveTrees().size() / 2) {
            _waveBonus += _difficulty + 1;
        }

        // if there isn't time to start another wave, end the game
        if (bangobj.lastTick - tick < MIN_WAVE_TICKS) {
            _treedel.clearTrees();
            bangobj.setLastTick(tick);
            return;
        }

        // if at least half the trees were saved, increase the difficulty level
        if (grown >= _treedel.getWaveTrees().size() / 2) {
            _difficulty = Math.min(_difficulty + 1, MAX_DIFFICULTY);
        }

        // reset the trees and choose a new subset
        _treedel.resetTrees(bangobj, _difficulty);

        // respawn all player units on the next tick
        _respdel.respawnAll(tick);

        // count down towards starting the next wave
        _nextWaveTick = tick + NEXT_WAVE_TICKS;
    }

    /**
     * Starts the next wave of logging robots.
     */
    protected void startNextWave (BangObject bangobj)
    {
        // announce the start of the wave
        _bangmgr.deployEffect(-1, new RobotWaveEffect(++_wave, _difficulty));

        // notify the logging robot delegate
        _logrobdel.waveStarted(bangobj, _wave, _difficulty, _treedel.getWaveTrees(), _treesGrown);
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

    @Override // documentation inherited
    public float getScaleFactor ()
    {
        return 0f;
    }

    /** The delegate responsible for respawning player units. */
    protected RespawnDelegate _respdel;

    /** The delegate that spawns and controls logging robots. */
    protected LoggingRobotDelegate _logrobdel;

    /** The delegate that manages the tree beds. */
    protected TreeBedDelegate _treedel;

    /** The payouts for each player, determined at the end of the round. */
    protected int[] _payouts;

    /** Used to track the spawn locations for the fetishes. */
    protected PointSet _fetishSpots = new PointSet();

    /** The current wave of robots. */
    protected int _wave;

    /** The difficulty level of the current wave. */
    protected int _difficulty;

    /** The accumulated number of trees grown in the completed waves. */
    protected int _treesGrown;

    /** The possible number of points that could have been accumulated. */
    protected int _waveMax;

    /** The tick at which to start the next wave. */
    protected int _nextWaveTick;

    /** The wave completed payout bonus. */
    protected int _waveBonus;

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

    /** The increment in the point multiplier (which starts at 1) for each
     * difficulty level. */
    protected static final float POINT_MULTIPLIER_INCREMENT = 1 / 9f;

    /** Payout adjustments for rankings in 2/3/4 player games. */
    protected static final int[][] PAYOUT_ADJUSTMENTS = {
        { -5, +5 }, { -10, 0, +10 }, { -10, -5, +5, +10 } };

    protected static final StatType[] ACCUM_STATS = {
        StatType.HARD_ROBOT_KILLS, StatType.PERFECT_WAVES };
}
