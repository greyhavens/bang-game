//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;

import com.threerings.media.util.MathUtil;
import com.threerings.util.MessageBundle;

import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.parlor.game.data.GameAI;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.data.Stat;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.GoldLogic;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li> Each player has a mine shaft, and those mine shafts start with a
 * particular quantity of gold.
 * <li> When another player's unit lands on (or in front of) the mine shaft,
 * they steal a nugget of gold from the shaft and must return that nugget to
 * their own shaft to deposit it.
 * <li> If the unit carrying the nugget is killed, it drops the nugget in a
 * nearby square and the nugget can then be picked up by any piece that lands
 * on it.
 * <li> When one player's mine is completely depleted of nuggets, the end of
 * round tick is advanced a bit.
 * <li> Any units that are killed during the round respawn near the player's
 * starting marker.
 * </ul>
 */
public class ClaimJumping extends GoldScenario
{
    /** The number of nuggets in each claim. TODO: put in BangConfig. */
    public static final int NUGGET_COUNT = 2;

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new GoldLogic(true);
    }
    
    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> starts,
                                PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // locate all the claims, assign them to players and fill them with
        // nuggets
        assignCounters(bangobj, starts, NUGGET_COUNT);

        // sort the bonus spots by distance to nearest claim, put up to one
        // nugget per player in the spots that are maximally distant from the
        // nearest claim
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

        int placed = 0;
        for (BonusSorter sorter : sorters) {
            Bonus nugget = dropNugget(bangobj, _bonusSpots.getX(sorter.index),
                                      _bonusSpots.getY(sorter.index));
            // we need to mark these nuggets as "occupying" the bonus spots
            // they are being dropped in, lest the server stick another bonus
            // in their place
            nugget.spot = sorter.index;

            // stop when we've placed one nugget for each player
            if (++placed >= bangobj.players.length) {
                break;
            }
        }
    }

    @Override // documentation inherited
    public void tick (BangObject bangobj, short tick)
    {
        super.tick(bangobj, tick);

        // if we're not at least half-way through the round, don't do our empty
        // claim calculations
        if (bangobj.tick < bangobj.duration/2) {
            return;
        }

        // check to see if there are empty claims
        boolean empty = false;
        for (Counter claim : _counters) {
            if (claim.count == 0) {
                empty = true;
                break;
            }
        }

        // if we are not already ending early, and one or more claims are
        // empty, adjust the lastTick...
        short realLastTick = (short)(bangobj.duration - 1);
        if (bangobj.lastTick == realLastTick && empty) {
            short lastTick = (short)(tick + EMPTY_CLAIM_TICKS);
            if (lastTick < realLastTick) {
                bangobj.setLastTick(lastTick);
            }

        // ...if no claims are empty clear the early ending tick
        } else if (bangobj.lastTick != realLastTick && !empty) {
            bangobj.setLastTick(realLastTick);
        }
    }

    @Override // documentation inherited
    public void roundDidEnd (BangObject bangobj)
    {
        super.roundDidEnd(bangobj);

        // increment each players' nugget related stats
        for (Counter claim : _counters) {
            if (claim.count > 0) {
                bangobj.stats[claim.owner].incrementStat(
                    Stat.Type.NUGGETS_CLAIMED, claim.count);
            }
        }
    }

    @Override // documentation inherited
    public void recordStats (
        BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(bangobj, gameTime, pidx, user);

        // record the number of nuggets they claimed
        int nuggets = bangobj.stats[pidx].getIntStat(Stat.Type.NUGGETS_CLAIMED);
        if (nuggets > 0) {
            user.stats.incrementStat(Stat.Type.NUGGETS_CLAIMED, nuggets);
        }
    }

    @Override // documentation inherited
    protected boolean respawnPieces ()
    {
        return true;
    }

    /**
     * Drops a nugget at the specified location.
     */
    protected static Bonus dropNugget (BangObject bangobj, int x, int y)
    {
        Bonus drop = Bonus.createBonus(
            BonusConfig.getConfig(NuggetEffect.NUGGET_BONUS));
        drop.assignPieceId(bangobj);
        drop.position(x, y);
        bangobj.board.shadowPiece(drop);
        bangobj.addToPieces(drop);
        return drop;
    }

    protected static class BonusSorter implements Comparable<BonusSorter>
    {
        /** The index of this bonus spot. */
        public short index;

        /** The distance (squared) from the bonus spot to the closest player. */
        public int minDistSq;

        /** Compare based on distance to nearest player. */
        public int compareTo (BonusSorter other) {
            return other.minDistSq - minDistSq;
        }
    }

    /** Indicates the tick on which we will end the game. */
    protected short _gameOverTick = -1;

    /** The number of ticks after which we end the game if at least one claim
     * remains empty for that duration. */
    protected static final int EMPTY_CLAIM_TICKS = 28;
}
