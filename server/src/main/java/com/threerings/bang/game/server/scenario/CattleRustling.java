//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.samskivert.util.RandomUtil;

import com.threerings.presents.server.InvocationException;
import com.threerings.parlor.game.data.GameAI;
import com.threerings.stats.data.StatSet;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.CountEffect;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.scenario.CattleRustlingInfo;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.CattleRustlingLogic;
import com.threerings.bang.game.util.PieceSet;

import static com.threerings.bang.Log.log;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li> Various cattle start scattered randomly around the board.
 * <li> When a player moves their Big Shot unit next to a cow, the Big Shot
 * brands that cow for the player. The cow remains branded by that player until
 * another player's Big Shot rebrands it.
 * <li> Any units that are killed during the round respawn near the
 * player's starting marker.
 * <li> The round ends after a fixed time limit.
 * <li> Players earn money for all cattle with their brand at the end of the
 * round.
 * </ul>
 */
public class CattleRustling extends Scenario
{
    /**
     * Creates a cattle rustling scenario and registers its delegates.
     */
    public CattleRustling ()
    {
        registerDelegate(new TrainDelegate());
        registerDelegate(new CattleDelegate());
        registerDelegate(new RustlingPostDelegate());
        registerDelegate(new RespawnDelegate());
    }

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new CattleRustlingLogic();
    }

    @Override // documentation inherited
    public void filterPieces (BangObject bangobj, Piece[] starts, List<Piece> pieces,
                              List<Piece> updates)
    {
        super.filterPieces(bangobj, starts, pieces, updates);

        // extract and remove all cattle markers
        _cattleSpots.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.CATTLE)) {
                _cattleSpots.add((Marker)p);
                iter.remove();
            }
        }
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // now place the cattle near the cattle starting spots
        int placed = 0, cattle = CATTLE_PER_PLAYER * bangobj.players.length;

        // freak out if the board is improperly configured
        if (_cattleSpots.size() == 0) {
            log.warning("Board has no cattle spots!", "board", _bangmgr.getBoardInfo());
            return;
        }

        log.debug("Placing " + cattle + " cattle in " + _cattleSpots.size() + " spots.");

        // We want to evenly distribute the cattle over the markers, so we'll
        // loop through the list of spots, adding one cow to each, until we've
        // reached the desired amount
        Collections.shuffle(_cattleSpots);
      PLACER_LOOP:
        while (placed < cattle) {
            for (Marker cspot : _cattleSpots) {
                List<Point> spots = bangobj.board.getOccupiableSpots(20, cspot.x, cspot.y, 3);
                Point spot = null;
                // we don't want to start cows on any tracks
                for (Point pt : spots) {
                    if (!bangobj.getTracks().containsKey(Piece.coord(pt.x, pt.y))) {
                        spot = pt;
                        break;
                    }
                }
                if (spot == null) {
                    continue;
                }

                Cow cow = new Cow();
                cow.assignPieceId(bangobj);
                cow.position(spot.x, spot.y);
                cow.orientation = (short)RandomUtil.getInt(4);
                _bangmgr.addPiece(cow);
                log.debug("Placed " + cow + ".");

                // stop when we've placed the desired number of cattle
                if (++placed >= cattle) {
                    break PLACER_LOOP;
                }
            }
        }
    }

    @Override // documentation inherited
    public void recordStats (StatSet[] stats, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(stats, gameTime, pidx, user);

        // propagate this player's cattle related stats into their permanent stat set
        user.stats.incrementStat(
            StatType.CATTLE_RUSTLED, stats[pidx].getIntStat(StatType.CATTLE_RUSTLED));
        user.stats.maxStat(
            StatType.MOST_CATTLE, stats[pidx].getIntStat(StatType.MOST_CATTLE));
    }

    @Override // documentation inherited
    protected short getBaseDuration ()
    {
        // cattle herding should be a bit shorter than the normal scenario
        return 4 * BASE_SCENARIO_TICKS / 5;
    }

    /**
     * Returns the owner of this cow (the player with the closest starting
     * spot) or -1 if the cow is not close enough to any spot to be owned.
     */
    protected int determineOwner (Cow cow)
    {
        int newOwner = -1;
        int minDist = MAX_OWNER_DISTANCE+1;

        // if the cow currently has an owner, and that owner is within range, start with them as
        // the new owner and only assign another player to be the owner if they are yet closer
        if (cow.owner != -1) {
            int oldDist = cow.getDistance(_startSpots[cow.owner].x, _startSpots[cow.owner].y);
            if (oldDist <= MAX_OWNER_DISTANCE) {
                newOwner = cow.owner;
                minDist = oldDist;
            }
        }

        for (int ii = 0; ii < _startSpots.length; ii++) {
            int dist = cow.getDistance(_startSpots[ii].x, _startSpots[ii].y);
            if (dist < minDist) {
                newOwner = ii;
                minDist = dist;
            }
        }
        return newOwner;
    }

    protected static class RustlingPostDelegate extends CounterDelegate
    {
        @Override // documentation inherited
        public void roundWillStart (BangObject bangobj)
            throws InvocationException
        {
            super.roundWillStart(bangobj);
            _counts = new int[_counters.size()];
        }

        @Override // documentation inherited
        public boolean tick (BangObject bangobj, short tick) {
            // if we have no counters, then avoid freakoutage
            if (_counters.size() == 0) {
                return false;
            }

            // update the counters with new values
            Arrays.fill(_counts, 0);
            for (Piece piece : bangobj.getPieceArray()) {
                if (piece instanceof Cow && piece.owner != -1) {
                    _counts[piece.owner]++;
                }
            }
            for (Counter counter : _counters) {
                int cattle = _counts[counter.owner];
                if (counter.count != cattle) {
                    _bangmgr.deployEffect(-1, CountEffect.changeCount(counter.pieceId, cattle));
                }
                // you get points for your branded cows at each tick
                int points = cattle * CattleRustlingInfo.POINTS_PER_BRAND;
                bangobj.stats[counter.owner].incrementStat(StatType.BRAND_POINTS, points);
                bangobj.grantPoints(counter.owner, points);
                // update this player's current cattle rustled stats
                bangobj.stats[counter.owner].setStat(StatType.CATTLE_RUSTLED, cattle);
                bangobj.stats[counter.owner].maxStat(StatType.MOST_CATTLE, cattle);
            }
            return false;
        }

        @Override // documentation inherited
        protected int pointsPerCounter () {
            return CattleRustlingInfo.POINTS_PER_COW;
        }

        @Override // documentation inherited
        protected void checkAdjustedCounter (BangObject bangobj, Unit unit) {
            // nothing to do here
        }

        protected int[] _counts;
    }

    protected List<Marker> _cattleSpots = Lists.newArrayList();

    protected static final int MAX_OWNER_DISTANCE = 5;
    protected static final int CATTLE_PER_PLAYER = 3;
}
