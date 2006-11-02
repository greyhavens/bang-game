//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.samskivert.util.RandomUtil;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;

import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.RandomLogic;
import com.threerings.bang.game.server.ai.WendigoLogic;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.TalismanEffect;

import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;

import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * A gameply scenario wherein:
 * <ul>
 * <li>Fill this in when the scenario is finalized.
 * </ul>
 */
public class WendigoAttack extends Scenario
    implements PieceCodes
{
    /**
     * Creates a wendigo attack scenario and registers its delegates.
     */
    public WendigoAttack ()
    {
        registerDelegate(_wendel = new WendigoDelegate());
        // respawn in half the time as normal
        int rticks = RespawnDelegate.RESPAWN_TICKS/2;
        registerDelegate(new RespawnDelegate(rticks, false) {
            public void pieceWasKilled (
                BangObject bangobj, Piece piece, int shooter) {
                int oldRT = _respawnTicks;
                // if units were killed by a wendigo they respawn quicker
                if (_wendel._wendigoRespawnTicks != null && piece.owner != -1) {
                    int pticks = _wendel._wendigoRespawnTicks[piece.owner]++;
                    _respawnTicks = Math.min(pticks, _respawnTicks);
                }
                super.pieceWasKilled(bangobj, piece, shooter);
                _respawnTicks = oldRT;
            }
        });

        _nextWendigo = (short)RandomUtil.getInt(
            MAX_WENDIGO_TICKS, MIN_WENDIGO_TICKS);
    }

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new WendigoLogic(this);
    }

    @Override // documentation inherited
    public void filterPieces (
        BangObject bangobj, ArrayList<Piece> starts, ArrayList<Piece> pieces,
        ArrayList<Piece> updates)
    {
        super.filterPieces(bangobj, starts, pieces, updates);

        // extract and remove the talisman spots
        _talismanSpots.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.TALISMAN)) {
                _talismanSpots.add(p.x, p.y);
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

        for (int ii = 0, nn = _talismanSpots.size(); ii < nn; ii++) {
            Bonus talisman = dropBonus(bangobj, TalismanEffect.TALISMAN_BONUS,
                _talismanSpots.getX(ii), _talismanSpots.getY(ii));
        }
    }

    @Override // documentation inherited
    public void tick (BangObject bangobj, short tick)
    {
        super.tick(bangobj, tick);
        if (tick < _nextWendigo) {
            return;
        }

        // if the wendigo are ready, we deploy 'em, otherwise we create them
        // which fades the board to let the players know they're coming
        if (_wendel.wendigoReady()) {
            _wendel.deployWendigo(bangobj, tick);
            if (_nextWendigo + MAX_WENDIGO_TICKS + MIN_WENDIGO_TICKS + 
                WENDIGO_WAIT * 2 < bangobj.duration) {
                _nextWendigo += (short)RandomUtil.getInt(
                    MAX_WENDIGO_TICKS, MIN_WENDIGO_TICKS);
            } else {
                _nextWendigo = (short)(bangobj.duration - WENDIGO_WAIT - 1);
            }

        } else {
            _wendel.prepareWendigo(bangobj, tick);
            _nextWendigo += WENDIGO_WAIT;
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

        // record the talisman on safe spot survivals
        survivals = bangobj.stats[pidx].getIntStat(
                Stat.Type.TALISMAN_SPOT_SURVIVALS);
        if (survivals > 0) {
            user.stats.incrementStat(
                    Stat.Type.TALISMAN_SPOT_SURVIVALS, survivals);
        }

        // record the whole team survivals
        survivals = bangobj.stats[pidx].getIntStat(
                Stat.Type.WHOLE_TEAM_SURVIVALS);
        if (survivals > 0) {
            user.stats.incrementStat(
                    Stat.Type.WHOLE_TEAM_SURVIVALS, survivals);
        }
    }

    /**
     * Returns the set of safe spots on the board.
     */
    public PointSet getSafeSpots ()
    {
        return _wendel.getSafeSpots();
    }

    /**
     * Returns true if the wendigo are prepared.
     */
    public boolean areWendigoPrepared ()
    {
        return _wendel.wendigoReady();
    }

    /** Handles the Wendigo and safe spots. */
    protected WendigoDelegate _wendel;

    /** The tick when the next wendigo will spawn. */
    protected short _nextWendigo;

    /** Used to track the locations of all talisman spots. */
    protected PointSet _talismanSpots = new PointSet();

    /** The sacred locations. */
    protected static final String SACRED_LOCATION =
        "indian_post/special/sacred_location";

    /** Number of ticks before wendigo appears. */
    protected static final short MIN_WENDIGO_TICKS = 9;
    protected static final short MAX_WENDIGO_TICKS = 15;

    /** Number of ticks after wendigo appear before they attack. */
    protected static final short WENDIGO_WAIT = 4;
}
