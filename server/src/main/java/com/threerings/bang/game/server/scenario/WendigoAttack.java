//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.List;
import java.util.Iterator;

import com.samskivert.util.RandomUtil;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.presents.server.InvocationException;

import com.threerings.stats.data.StatSet;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;

import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.WendigoLogic;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.TalismanEffect;

import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;

import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

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
        registerDelegate(_resdel = new RespawnDelegate(rticks, false) {
            public void pieceWasKilled (
                BangObject bangobj, Piece piece, int shooter, int sidx) {
                int oldRT = _respawnTicks;
                // if units were killed by a wendigo they respawn quicker
                if (_wendel._wendigoRespawnTicks != null && piece.owner != -1) {
                    int pticks = _wendel._wendigoRespawnTicks[piece.owner]++;
                    _respawnTicks = Math.min(pticks, _respawnTicks);
                }
                super.pieceWasKilled(bangobj, piece, shooter, sidx);
                _respawnTicks = oldRT;
            }
        });

        _nextWendigo = (short)RandomUtil.getInRange(MIN_WENDIGO_TICKS, MAX_WENDIGO_TICKS + 1);
    }

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new WendigoLogic(this);
    }

    @Override // documentation inherited
    public void filterPieces (BangObject bangobj, Piece[] starts, List<Piece> pieces,
                              List<Piece> updates)
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
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        for (int ii = 0, nn = _talismanSpots.size(); ii < nn; ii++) {
            dropBonus(bangobj, TalismanEffect.TALISMAN_BONUS,
                _talismanSpots.getX(ii), _talismanSpots.getY(ii));
        }
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        boolean validate = false;
        // if the wendigo are ready, we deploy 'em, otherwise we create them
        // which fades the board to let the players know they're coming
        if (tick >= _nextWendigo) {
            if (_wendel.wendigoReady()) {
                _wendel.deployWendigo(bangobj, tick);
                if (_nextWendigo + MAX_WENDIGO_TICKS + MIN_WENDIGO_TICKS +
                    WENDIGO_WAIT * 2 < bangobj.duration) {
                    _nextWendigo += (short)RandomUtil.getInRange(
                            MIN_WENDIGO_TICKS, MAX_WENDIGO_TICKS + 1);
                } else {
                    _nextWendigo = (short)(bangobj.duration - WENDIGO_WAIT - 1);
                }
                // allow the game config to override respawning
                _resdel.setRespawn(((BangConfig)_bangmgr.getConfig()).respawnUnits);
                validate = true;

            } else {
                _wendel.prepareWendigo(bangobj, tick);
                _nextWendigo += WENDIGO_WAIT;
                _resdel.setRespawn(false);
            }
        }
        return super.tick(bangobj, tick) || validate;
    }

    @Override // documentation inherited
    public void recordStats (
        StatSet[] stats, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(stats, gameTime, pidx, user);

        // record the number of wendigo survivals
        int survivals = stats[pidx].getIntStat(StatType.WENDIGO_SURVIVALS);
        if (survivals > 0) {
            user.stats.incrementStat(StatType.WENDIGO_SURVIVALS, survivals);
        }

        // record the talisman on safe spot survivals
        survivals = stats[pidx].getIntStat(StatType.TALISMAN_SPOT_SURVIVALS);
        if (survivals > 0) {
            user.stats.incrementStat(StatType.TALISMAN_SPOT_SURVIVALS, survivals);
        }

        // record the whole team survivals
        survivals = stats[pidx].getIntStat(StatType.WHOLE_TEAM_SURVIVALS);
        if (survivals > 0) {
            user.stats.incrementStat(StatType.WHOLE_TEAM_SURVIVALS, survivals);
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

    /** Handles respawning dead units. */
    protected RespawnDelegate _resdel;

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
