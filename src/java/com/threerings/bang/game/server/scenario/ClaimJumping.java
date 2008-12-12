//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.samskivert.util.RandomUtil;

import com.threerings.parlor.game.data.GameAI;
import com.threerings.presents.server.InvocationException;
import com.threerings.stats.data.StatSet;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.GoldLogic;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

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
public class ClaimJumping extends Scenario
{
    /** The number of nuggets in each claim. TODO: put in BangConfig. */
    public static final int NUGGET_COUNT = 2;

    /**
     * Creates a claim jumping scenario and registers its delegates.
     */
    public ClaimJumping ()
    {
        registerDelegate(new TrainDelegate());
        registerDelegate(new NuggetDelegate(true, NUGGET_COUNT));
        registerDelegate(new RespawnDelegate());
    }

    @Override // documentation inherited
    public void filterPieces (BangObject bangobj, Piece[] starts, List<Piece> pieces,
                              List<Piece> updates)
    {
        super.filterPieces(bangobj, starts, pieces, updates);

        _lodeSpots.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.LODE)) {
                _lodeSpots.add(p.x, p.y);
                iter.remove();
            }
        }
    }

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new GoldLogic(true);
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        int[] weights = new int[_lodeSpots.size()];
        Arrays.fill(weights, 1);

        for (int ii = 0; (ii < bangobj.players.length) &&
                    (ii < weights.length); ii++) {
            int idx = RandomUtil.getWeightedIndex(weights);
            dropBonus(
                bangobj, NuggetEffect.NUGGET_BONUS, _lodeSpots.getX(idx), _lodeSpots.getY(idx));
            weights[idx] = 0;
        }
    }

    @Override // documentation inherited
    public void recordStats (
        StatSet[] stats, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(stats, gameTime, pidx, user);

        // record the number of nuggets they claimed and update their max claimed
        user.stats.incrementStat(
            StatType.NUGGETS_CLAIMED, stats[pidx].getIntStat(StatType.NUGGETS_CLAIMED));
    }

    /** Indicates the tick on which we will end the game. */
    protected short _gameOverTick = -1;

    /** Used to track the locations of the starting nuggets. */
    protected PointSet _lodeSpots = new PointSet();

    /** The number of ticks after which we end the game if at least one claim
     * remains empty for that duration. */
    protected static final int EMPTY_CLAIM_TICKS = 28;
}
