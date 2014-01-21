//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import com.google.common.collect.Lists;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.IronPlateEffect;
import com.threerings.bang.game.data.effect.ResurrectEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Revolutionary;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Handles the respawning of units.
 */
public class RespawnDelegate extends ScenarioDelegate
{
    /** The default number of ticks before a unit respawns. */
    public static final int RESPAWN_TICKS = 12;

    public RespawnDelegate ()
    {
        this(RESPAWN_TICKS);
    }

    public RespawnDelegate (int respawnTicks)
    {
        this(respawnTicks, true);
    }

    public RespawnDelegate (int respawnTicks, boolean freeIronPlate)
    {
        _respawnTicks = respawnTicks;
        _freeIronPlate = freeIronPlate;
    }

    /**
     * Sets all units to respawn at the next tick.
     */
    public void respawnAll (short tick)
    {
        for (Unit unit : _respawns) {
            unit.setRespawnTick(tick);
        }
    }

    /**
     * Controls if units will respawn.
     */
    public void setRespawn (boolean respawn)
    {
        _respawn = respawn;
    }

    @Override // from ScenarioDelegate
    public void roundWillStart (BangObject bangobj)
    {
        // clear our respawn queue
        _respawns.clear();

        // disable respawning if the configuration indicates to do so
        _respawn = ((BangConfig)_bangmgr.getConfig()).respawnUnits;
    }

    @Override // from ScenarioDelegate
    public boolean tick (BangObject bangobj, short tick)
    {
        boolean validate = false;
        // respawn new pieces
        while (_respawn && _respawns.size() > 0) {
            if (_respawns.peek().getRespawnTick() > tick) {
                break;
            }

            Unit unit = _respawns.poll();
            log.debug("Respawning " + unit + ".");

            // figure out where to put this guy
            Point spot = _parent.getStartSpot(unit.owner);
            Point bspot = bangobj.board.getOccupiableSpot(spot.x, spot.y, 3);
            if (bspot == null) {
                log.warning("Unable to locate spawn spot for to-be-respawned unit", "unit", unit,
                            "spot", spot);
                // stick him back on the queue for a few ticks later
                unit.setRespawnTick((short)(tick + _respawnTicks));
                _respawns.add(unit);
                continue;
            }

            // reassign the unit to its original owner
            unit.setOwner(bangobj, unit.originalOwner);

            // reset the units vital statistics
            unit.respawnInit(bangobj);

            // if the unit is still in play for some reason, remove it first
            if (bangobj.pieces.containsKey(unit.getKey())) {
                bangobj.board.clearShadow(unit);
                bangobj.removeFromPieces(unit.getKey());
            }

            // don't respawn units for players that are no longer active
            if (!_bangmgr.isActivePlayer(unit.owner)) {
                continue;
            }

            boolean freeIronPlate = _freeIronPlate && !bangobj.hasLiveUnits(unit.owner);

            // then position it and add it back at its new location
            unit.position(bspot.x, bspot.y);
            _bangmgr.addPiece(unit, AddPieceEffect.RESPAWNED);
            validate = true;

            if (freeIronPlate) {
                IronPlateEffect effect = new IronPlateEffect();
                effect.freebie = true;
                effect.init(unit);
                _bangmgr.deployEffect(-1, effect);
            }
        }

        return validate;
    }

    @Override // from ScenarioDelegate
    public void pieceWasKilled (BangObject bangobj, Piece piece, int shooter, int sidx)
    {
        if (!(piece instanceof Unit) || ((Unit)piece).originalOwner == -1) {
            return;
        }
        Unit unit = (Unit)piece;
        unit.setRespawnTick((short)(bangobj.tick + _respawnTicks));

        // the Revolutionary will cause all allied units to respawn on the next tick
        if (unit instanceof Revolutionary) {
            List<Unit> saved = Lists.newArrayList();
            for (Iterator<Unit> iter = _respawns.iterator(); iter.hasNext(); ) {
                Unit u = iter.next();
                if (u.owner == unit.owner) {
                    iter.remove();
                    u.setRespawnTick(bangobj.tick);
                    saved.add(u);
                }
            }
            _respawns.addAll(saved);
        }

        _respawns.add(unit);
        log.debug("Queued for respawn " + unit + ".");
    }

    @Override // documentation inherited
    public void pieceAffected (Piece piece, String effect)
    {
        // remove resurrected units from the respawn queue
        if (ResurrectEffect.RESURRECTED.equals(effect) && piece instanceof Unit) {
            _respawns.remove(piece);
        }
    }

    /** A list of units waiting to be respawned. */
    protected PriorityQueue<Unit> _respawns = new PriorityQueue<Unit>(10, new Comparator<Unit>() {
        public int compare (Unit u1, Unit u2) {
            return u1.getRespawnTick() - u2.getRespawnTick();
        }
        public boolean equals(Object obj) {
            return false;
        }
    });

    /** The number of ticks that must elapse before a unit is respawned. */
    protected int _respawnTicks = RESPAWN_TICKS;

    /** Whether units will actually respawn. */
    protected boolean _respawn = true;

    /** If we're giving out the free iron plates. */
    protected boolean _freeIronPlate;
}
