//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import com.samskivert.util.RandomUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;

import com.threerings.bang.game.data.BangConfig;
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
import com.threerings.bang.game.util.PointSet;

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
    public void filterPieces (
        BangObject bangobj, ArrayList<Piece> starts, ArrayList<Piece> pieces,
        ArrayList<Piece> updates)
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
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> starts,
                                PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // now place the cattle near the cattle starting spots
        int placed = 0, cattle = CATTLE_PER_PLAYER * bangobj.players.length;

        // freak out if the board is improperly configured
        if (_cattleSpots.size() == 0) {
            log.warning("Board has no cattle spots! " +
                "[name=" + bangobj.boardName +
                ", pcount=" + bangobj.players.length + "].");
            return;
        }

        log.fine("Placing " + cattle + " cattle in " +
                 _cattleSpots.size() + " spots.");

        // We want to evenly distribute the cattle over the markers, so we'll
        // loop through the list of spots, adding one cow to each, until we've
        // reached the desired amount
        Collections.shuffle(_cattleSpots);
      PLACER_LOOP:
        while (placed < cattle) {
            for (Marker cspot : _cattleSpots) {
                Point spot = bangobj.board.getOccupiableSpot(
                        cspot.x, cspot.y, 3);
                if (spot == null) {
                    continue;
                }

                Cow cow = new Cow();
                cow.assignPieceId(bangobj);
                cow.position(spot.x, spot.y);
                cow.orientation = (short)RandomUtil.getInt(4);
                _bangmgr.addPiece(cow);
                log.fine("Placed " + cow + ".");

                // stop when we've placed the desired number of cattle
                if (++placed >= cattle) {
                    break PLACER_LOOP;
                }
            }
        }
    }

    @Override // documentation inherited
    public void roundDidEnd (BangObject bangobj)
    {
        super.roundDidEnd(bangobj);

        // note rustled counts
        Piece[] pieces = bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof Cow && pieces[ii].owner != -1) {
                bangobj.stats[pieces[ii].owner].incrementStat(Stat.Type.CATTLE_RUSTLED, 1);
            }
        }

        // TODO: update MOST_CATTLE during the round as cattle change hands
    }

    @Override // documentation inherited
    public void recordStats (
        BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(bangobj, gameTime, pidx, user);

        // record their cattle related stats
        user.stats.incrementStat(
            Stat.Type.CATTLE_RUSTLED, bangobj.stats[pidx].getIntStat(Stat.Type.CATTLE_RUSTLED));
        user.stats.maxStat(
            Stat.Type.MOST_CATTLE, bangobj.stats[pidx].getIntStat(Stat.Type.MOST_CATTLE));
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

        // if the cow currently has an owner, and that owner is within
        // range, start with them as the new owner and only assign another
        // player to be the owner if they are closer than this player
        if (cow.owner != -1) {
            int oldDist = cow.getDistance(_startSpots[cow.owner].x,
                                          _startSpots[cow.owner].y);
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
        public void tick (BangObject bangobj, short tick) {
            // if we have no counters, then avoid freakoutage
            if (_counters.size() == 0) {
                return;
            }

            // update the counters with new values
            Arrays.fill(_counts, 0);
            Piece[] pieces = bangobj.getPieceArray();
            for (int ii = 0; ii < pieces.length; ii++) {
                if (pieces[ii] instanceof Cow && pieces[ii].owner != -1) {
                    _counts[pieces[ii].owner]++;
                }
            }
            for (Counter counter : _counters) {
                if (counter.count != _counts[counter.owner]) {
                    _bangmgr.deployEffect(
                        -1, CountEffect.changeCount(
                            counter.pieceId, _counts[counter.owner]));
                }
                // you get points for your branded cows at each tick
                int points = _counts[counter.owner] * 
                    CattleRustlingInfo.POINTS_PER_BRAND;
                bangobj.stats[counter.owner].incrementStat(
                        Stat.Type.BRAND_POINTS, points); 
                bangobj.grantPoints(counter.owner, points);
            }
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

    protected ArrayList<Marker> _cattleSpots = new ArrayList<Marker>();

    protected static final int MAX_OWNER_DISTANCE = 5;
    protected static final int CATTLE_PER_PLAYER = 3;
}
