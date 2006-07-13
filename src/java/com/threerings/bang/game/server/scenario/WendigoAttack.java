//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;

import java.awt.Rectangle;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;

import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.RandomLogic;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;

import com.threerings.bang.game.data.effect.TalismanEffect;
import com.threerings.bang.game.data.effect.WendigoEffect;

import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Wendigo;

import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.presents.server.InvocationException;

import static com.threerings.bang.Log.log;

/**
 * A gameply scenario wherein:
 * <ul>
 * <li>Fill this in when the scenario is finalized.
 * </ul>
 */
public class WendigoAttack extends Scenario
    implements ScenarioCodes, PieceCodes
{
    /**
     * Creates a wendigo attack scenario and registers its delegates.
     */
    public WendigoAttack ()
    {
        registerDelegate(new RespawnDelegate(RespawnDelegate.RESPAWN_TICKS/2));
        registerDelegate(new WendigoDelegate());
    }

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new RandomLogic();
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> starts,
                                PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        ArrayList<BonusSorter> sorters = sortBonusList();

        int placed = 1;
        for (BonusSorter sorter : sorters) {
            Bonus talisman = dropBonus(bangobj, TalismanEffect.TALISMAN_BONUS,
                _bonusSpots.getX(sorter.index), _bonusSpots.getY(sorter.index));
            // we need to mark these talismen as "occupying" the bonus spots
            // they are being dropped in, lest the server stick another bonus
            // in their place
            talisman.spot = sorter.index;

            // stop when we've placed one fewer talismen than players
            if (++placed >= bangobj.players.length) {
                break;
            }
        }
    }

    @Override // documentation inherited
    public void recordStats (
        BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(bangobj, gameTime, pidx, user);

        // record the number of wendigo survivals
        int survivals = bangobj.stats[pidx].getIntStat(
                Stat.Type.WENDIGO_SURVIVALS);
        if (survivals > 0) {
            user.stats.incrementStat(Stat.Type.WENDIGO_SURVIVALS, survivals);
        }
    }

    protected class WendigoDelegate extends ScenarioDelegate
    {
        public WendigoDelegate ()
        {
            _nextWendigo = (short)RandomUtil.getInt(
                    MAX_WENDIGO_TICKS, MIN_WENDIGO_TICKS);
        }

        @Override // documentation inherited
        public void roundWillStart (BangObject bangobj)
            throws InvocationException
        {
            _safePoints = new PointSet();
            Piece[] pieces = bangobj.getPieceArray();
            for (Piece p : pieces) {
                if (p instanceof Prop && 
                        SACRED_LOCATION.equals(((Prop)p).getType())) {
                    _safePoints.add(p.x, p.y);
                }
            }
        }

        @Override // documentation inherited
        public void tick (BangObject bangobj, short tick)
        {
            if (_wendigo != null) {
                if (_removeWendigo) {
                    bangobj.removeFromPieces(_wendigo.getKey());
                    _wendigo = null;
                    _removeWendigo = false;
                } else if (_wendigo.ticksUntilMovable(tick) <= 0) {
                    WendigoEffect effect = _wendigo.attack(bangobj);
                    effect.safePoints = _safePoints;
                    _bangmgr.deployEffect(-1, effect);
                    _removeWendigo = true;
                    updatePoints(bangobj);
                }
            }
            if (tick >= _nextWendigo) {
                createWendigo(bangobj, tick);
                _nextWendigo += (short)RandomUtil.getInt(
                        MAX_WENDIGO_TICKS, MIN_WENDIGO_TICKS);
            }
        }

        /**
         * Create a wendigo that will spawn just outside the playfield.
         */
        protected void createWendigo (BangObject bangobj, short tick)
        {
            // First decide horizontal or vertical attack
            int off = 0;
            boolean horiz = true;
            int length = 0;
            Rectangle playarea = bangobj.board.getPlayableArea();
            if (RandomUtil.getInt(2) == 0) {
                off = playarea.y;
                length = playarea.height - 1;
            } else {
                off = playarea.x;
                length = playarea.width - 1;
                horiz = false;
            }
            log.info("Creating Wendigo [playarea=" + playarea + ", horiz=" +
                    horiz + "].");

            // pick the set of tiles to attack based on the number of units
            // in the attack zone
            int[] weights = new int[length];
            Piece[] pieces = bangobj.getPieceArray();
            for (Piece p : pieces) {
                if (p instanceof Unit && p.isAlive()) {
                    int coord = (horiz ? p.y : p.x) - off;
                    if (coord < length) {
                        weights[coord]++;
                    }
                    if (coord - 1 >= 0) {
                        weights[coord - 1]++;
                    }
                }
            }
            int idx;
            idx = off + (IntListUtil.sum(weights) == 0 ?
                RandomUtil.getInt(length) :
                RandomUtil.getWeightedIndex(weights));
            _wendigo = new Wendigo();
            _wendigo.assignPieceId(bangobj);
            int orient = NORTH;
            if (horiz) {
                orient = (RandomUtil.getInt(2) == 0) ? EAST : WEST;
                _wendigo.position(
                    playarea.x + (orient == EAST ? -2 : playarea.width),
                    idx);
            } else {
                orient = (RandomUtil.getInt(2) == 0) ? NORTH : SOUTH;
                _wendigo.position(idx,
                    playarea.y + (orient == SOUTH ? -2 : playarea.height));
            }
            _wendigo.orientation = (short)orient;
            _wendigo.lastActed = tick;
            log.info("Wendigo Created [_wendigo=" + _wendigo + "].");
            bangobj.addToPieces(_wendigo);
        }

        /**
         * Grant points for surviving units after a wendigo attack.
         */
        protected void updatePoints (BangObject bangobj)
        {
            int[] points = new int[bangobj.players.length];
            Piece[] pieces = bangobj.getPieceArray();
            for (Piece p : pieces) {
                if (p instanceof Unit && p.isAlive() && p.owner > -1) {
                    points[p.owner]++;
                }
            }

            bangobj.startTransaction();
            try {
                for (int idx = 0; idx < points.length; idx++) {
                    if (points[idx] > 0) {
                        bangobj.grantPoints(idx, points[idx] *
                                ScenarioCodes.POINTS_PER_SURVIVAL);
                        bangobj.stats[idx].incrementStat(
                                Stat.Type.WENDIGO_SURVIVALS, points[idx]);
                    }
                }
            } finally {
                bangobj.commitTransaction();
            }
        }

        /** Our wendigo. */
        protected Wendigo _wendigo;

        /** The tick when the next wendigo will spawn. */
        protected short _nextWendigo;

        /** Set of the sacred location markers. */
        protected PointSet _safePoints;

        /** Whether to remove the wendigo on the next tick. */
        protected boolean _removeWendigo = false;

        /** Number of ticks before wendigo appears. **/
        protected static final short MIN_WENDIGO_TICKS = 10;
        protected static final short MAX_WENDIGO_TICKS = 16;

        /** The sacred locations. */
        protected static final String SACRED_LOCATION =
            "indian_post/special/sacred_location";
    }
}
