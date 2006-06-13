//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Handles the respawning of units.
 */
public class RespawnDelegate extends ScenarioDelegate
{
    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj)
    {
        // clear our respawn queue
        _respawns.clear();
    }

    @Override // documentation inherited
    public void tick (BangObject bangobj, short tick)
    {
        // respawn new pieces
        while (_respawns.size() > 0) {
            if (_respawns.get(0).getRespawnTick() > tick) {
                break;
            }

            Unit unit = _respawns.remove(0);
            log.fine("Respawning " + unit + ".");

            // reassign the unit to its original owner
            unit.owner = unit.originalOwner;

            // figure out where to put this guy
            Point spot = _parent._startSpots[unit.owner];
            Point bspot = bangobj.board.getOccupiableSpot(spot.x, spot.y, 3);
            if (bspot == null) {
                log.warning("Unable to locate spawn spot for to-be-respawned " +
                            "unit [unit=" + unit + ", spot=" + spot + "].");
                // stick him back on the queue for a few ticks later
                unit.setRespawnTick((short)(tick + RESPAWN_TICKS));
                _respawns.add(unit);
                continue;
            }

            // reset the units vital statistics
            unit.damage = 0;
            unit.influence = null;
            unit.benuggeted = false;
            unit.setRespawnTick((short)0);

            // if the unit is still in play for some reason, remove it first
            if (bangobj.pieces.containsKey(unit.getKey())) {
                bangobj.board.clearShadow(unit);
                bangobj.removeFromPieces(unit.getKey());
            }

            // then position it and add it back at its new location
            unit.position(bspot.x, bspot.y);
            bangobj.addToPieces(unit);
            bangobj.board.shadowPiece(unit);
        }
    }

    @Override // documentation inherited
    public void pieceWasKilled (BangObject bangobj, Piece piece)
    {
        if (!(piece instanceof Unit) || ((Unit)piece).originalOwner == -1) {
            return;
        }
        Unit unit = (Unit)piece;
        unit.setRespawnTick((short)(bangobj.tick + RESPAWN_TICKS));
        _respawns.add(unit);
        log.fine("Queued for respawn " + unit + ".");
    }

    /** A list of units waiting to be respawned. */
    protected ArrayList<Unit> _respawns = new ArrayList<Unit>();

    /** The number of ticks that must elapse before a unit is respawned. */
    protected static final int RESPAWN_TICKS = 12;
}
