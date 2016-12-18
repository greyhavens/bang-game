//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.List;
import java.util.Arrays;
import java.util.Iterator;

import java.awt.Rectangle;

import com.google.common.collect.Lists;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.StatType;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.CountEffect;
import com.threerings.bang.game.data.effect.FadeBoardEffect;
import com.threerings.bang.game.data.effect.TalismanEffect;
import com.threerings.bang.game.data.effect.ToggleSwitchEffect;
import com.threerings.bang.game.data.effect.WendigoEffect;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.ToggleSwitch;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Wendigo;
import com.threerings.bang.game.data.scenario.TutorialInfo;
import com.threerings.bang.game.data.scenario.WendigoAttackInfo;
import com.threerings.bang.game.util.PointSet;

/**
 * Handles the heavy lifting for the {@link WendigoAttack} scenario.
 */
public class WendigoDelegate extends CounterDelegate
    implements PieceCodes
{
    /**
     * Returns the set of active safe points.
     */
    public PointSet getSafeSpots ()
    {
        return _safeSpots[_activeSafeSpots];
    }

    /**
     * Returns true if the wendigo are created and ready for deployment.
     */
    public boolean wendigoReady ()
    {
        return (_wendigo != null);
    }

    /**
     * Create several wendigo that will spawn just outside the playfield. Also
     * fades the board to let the players know the wendigo are coming. This
     * should be called prior to deploying them via {@link #deployWendigo}.
     */
    public void prepareWendigo (BangObject bangobj, short tick)
    {
        // fade the board to let the players know the wendigo are coming
        _bangmgr.deployEffect(-1, new FadeBoardEffect());

        // First decide horizontal or vertical attack
        Rectangle playarea = bangobj.board.getPlayableArea();
        // if we're in the tutorial, always do horizontal
        boolean horiz = bangobj.scenario.getIdent().equals(TutorialInfo.IDENT) ?
            true : (RandomUtil.getInt(2) == 0);

        int off = 0;
        int length = 0;
        if (horiz) {
            off = playarea.y;
            length = playarea.height;
        } else {
            off = playarea.x;
            length = playarea.width;
        }
        int idx = length / 2 - 1;
        _wendigo = Lists.newArrayListWithExpectedSize(3);

        boolean side = RandomUtil.getInt(2) == 0;
        int size = Math.min(length / 4, 3);
        _wendigo.add(createWendigo(bangobj, idx + off, horiz, side, playarea, false, tick));
        if (size > 0) {
            _wendigo.add(createWendigo(bangobj, idx+off-size, horiz, side, playarea, true, tick));
            _wendigo.add(createWendigo(bangobj, idx+off+size, horiz, side, playarea, true, tick));
        }
    }

    /**
     * Sends in the Wendigo.
     */
    public void deployWendigo (BangObject bangobj, short tick)
    {
        for (Wendigo wendigo : _wendigo) {
            _bangmgr.addPiece(wendigo);
        }
        WendigoEffect effect = WendigoEffect.wendigoAttack(bangobj, _wendigo);
        effect.safeSpots = getSafeSpots();
        _wendigoRespawnTicks = new int[bangobj.players.length];
        Arrays.fill(_wendigoRespawnTicks, 3);
        _bangmgr.deployEffect(-1, effect);
        _wendigoRespawnTicks = null;
        updatePoints(bangobj);
        _wendigo = null;
    }

    @Override // documentation inherited
    public void filterPieces (BangObject bangobj, Piece[] starts, List<Piece> pieces,
                              List<Piece> updates)
    {
        super.filterPieces(bangobj, starts, pieces, updates);

        // extract and remove all the safe spots
        _safeSpots[0].clear();
        _safeSpots[1].clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.SAFE)) {
                _safeSpots[0].add(p.x, p.y);
                // we don't remove the markers here since we want to assign
                // it a pieceId

            } else if (Marker.isMarker(p, Marker.SAFE_ALT)) {
                _safeSpots[1].add(p.x, p.y);

            } else if (p instanceof ToggleSwitch) {
                _toggleSwitches.add((ToggleSwitch)p);
            }
        }
    }

    @Override // documentation inherited
    public void pieceMoved (BangObject bangobj, Piece piece)
    {
        checkTSActivation(piece, bangobj.tick);

        for (ToggleSwitch ts : _toggleSwitches) {
            if (ts.x == piece.x && ts.y == piece.y) {
                ToggleSwitchEffect effect = new ToggleSwitchEffect();
                effect.switchId = ts.pieceId;
                effect.occupier = piece.pieceId;
                if (ts.isActive(bangobj.tick)) {
                    _activeSafeSpots = (_activeSafeSpots + 1) % 2;
                    effect.state = getTSState();
                    effect.activator = piece.pieceId;
                }
                _bangmgr.deployEffect(-1, effect);
                break;
            }
        }
    }

    @Override // documentation inherited
    public void pieceWasKilled (BangObject bangobj, Piece piece, int shooter, int sidx)
    {
        // a dirigible may activate a toggle on dying
        checkTSActivation(piece, bangobj.tick);
    }

    @Override // documentation inherited
    protected int pointsPerCounter ()
    {
        return WendigoAttackInfo.POINTS_PER_SURVIVAL;
    }

    @Override // documentation inherited
    protected void checkAdjustedCounter (BangObject bangobj, Unit unit)
    {
        // nothing to do here
    }

    protected ToggleSwitch.State getTSState ()
    {
        return (_activeSafeSpots == 0 ?
                ToggleSwitch.State.SQUARE : ToggleSwitch.State.CIRCLE);
    }

    /**
     * Checks for the activation of a toggle switch.
     */
    protected void checkTSActivation (Piece piece, short tick)
    {
        for (ToggleSwitch ts : _toggleSwitches) {
            if (piece.pieceId == ts.occupier) {
                ToggleSwitchEffect effect = new ToggleSwitchEffect();
                effect.switchId = ts.pieceId;
                effect.occupier = piece.pieceId;
                if (piece.pieceId == ts.activator) {
                    effect.tick = tick;
                } else {
                    effect.tick = -1;
                }
                _bangmgr.deployEffect(-1, effect);
            }
        }
    }

    /**
     * Creates and returns a particular Wendigo with the supplied settings.
     */
    protected Wendigo createWendigo (
        BangObject bangobj, int idx, boolean horiz, boolean side,
        Rectangle playarea, boolean claw, short tick)
    {
        Wendigo wendigo = new Wendigo(claw);
        wendigo.assignPieceId(bangobj);
        int orient = NORTH;
        if (horiz) {
            orient = side ? EAST : WEST;
            wendigo.position(
                playarea.x + (orient == EAST ? -4 : playarea.width + 2), idx);
        } else {
            orient = side ? NORTH : SOUTH;
            wendigo.position(
                idx, playarea.y + (orient == SOUTH ? -4 : playarea.height + 2));
        }
        wendigo.orientation = (short)orient;
        wendigo.lastActed = tick;
        return wendigo;
    }

    /**
     * Grant points for surviving units after a wendigo attack.
     */
    protected void updatePoints (BangObject bangobj)
    {
        int[] survivals = new int[bangobj.players.length];
        int[] talsurvivals = new int[bangobj.players.length];

        for (Piece p : bangobj.getPieceArray()) {
            if (p instanceof Unit && p.owner > -1) {
                if (p.isAlive()) {
                    survivals[p.owner]++;
                    if (getSafeSpots().contains(p.x, p.y) &&
                        TalismanEffect.TALISMAN_BONUS.equals(
                            ((Unit)p).holding)) {
                        talsurvivals[p.owner]++;
                    }
                }
            }
        }

        bangobj.startTransaction();
        try {
            for (int idx = 0; idx < survivals.length; idx++) {
                if (survivals[idx] > 0) {
                    int talpts = talsurvivals[idx] * TALISMAN_SAFE;
                    bangobj.grantPoints(idx, survivals[idx] * pointsPerCounter() + talpts);
                    bangobj.stats[idx].incrementStat(StatType.WENDIGO_SURVIVALS, survivals[idx]);
                    bangobj.stats[idx].incrementStat(StatType.TALISMAN_POINTS, talpts);
                    bangobj.stats[idx].incrementStat(
                        StatType.TALISMAN_SPOT_SURVIVALS, talsurvivals[idx]);
                    if (survivals[idx] == _bangmgr.getWholeTeamSize(idx) ) {
                        bangobj.stats[idx].incrementStat(StatType.WHOLE_TEAM_SURVIVALS, 1);
                    }
                }
            }
        } finally {
            bangobj.commitTransaction();
        }

        if (_counters.size() == 0) {
            return;
        }

        int queuePiece = _wendigo.get(0).pieceId;
        for (Counter counter : _counters) {
            if (survivals[counter.owner] > 0) {
                _bangmgr.deployEffect(
                        -1, CountEffect.changeCount(counter.pieceId,
                            counter.count + survivals[counter.owner],
                            queuePiece));
            }
        }
    }

    /** Our wendigo. */
    protected List<Wendigo> _wendigo;

    /** The tick when the wendigo will attack. */
    protected short _attackTick;

    /** Current wendigo attack number. */
    protected int _numAttacks;

    /** Respawn ticks for units. */
    protected int[] _wendigoRespawnTicks;

    /** Reference to the toggle switches. */
    protected List<ToggleSwitch> _toggleSwitches = Lists.newArrayList();

    /** Which set of safe spots are currently active. */
    protected int _activeSafeSpots = 0;

    /** Set of the sacred location markers. */
    protected PointSet[] _safeSpots = new PointSet[] { new PointSet(), new PointSet() };

    /** Number of extra points for having a talisman on a safe zone. */
    protected static final int TALISMAN_SAFE = 25;
}
