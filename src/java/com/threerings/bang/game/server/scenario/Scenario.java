//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;

import com.samskivert.util.IntIntMap;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Implements a particular gameplay scenario.
 */
public abstract class Scenario
{
    /**
     * Called when a round is about to start.
     *
     * @throws InvocationException containing a translatable string
     * indicating why the scenario is booched, which will be displayed to
     * the players and the game will be cancelled.
     */
    public void init (BangObject bangobj, ArrayList<Piece> markers,
                      PointSet bonusSpots, PieceSet purchases)
        throws InvocationException
    {
        // clear our respawn queue
        _respawns.clear();

        // this will contain the starting spot for each player
        _startSpots = new Point[bangobj.players.length];
        for (int ii = 0; ii < _startSpots.length; ii++) {
            Piece p = markers.get(ii);
            _startSpots[ii] = new Point(p.x, p.y);
        }

        // note the starting owner for all the purchased pieces; only
        // starting pieces are respawnable and they are returned to their
        // original owner when they respawn
        for (Piece piece : purchases.values()) {
            _startingOwners.put(piece.pieceId, piece.owner);
        }
    }

    /**
     * Called at the start of every game tick to allow the scenario to
     * affect the game state and determine whether or not the game should
     * be ended.
     *
     * @return true if the game should be ended, false if not.
     */
    public boolean tick (BangObject bangobj, short tick)
    {
        // respawn new pieces
        while (_respawns.size() > 0) {
            if (_respawns.get(0).getRespawnTick() > tick) {
                break;
            }

            Unit unit = _respawns.remove(0);
            log.info("Respawning " + unit + ".");

            // figure out where to put this guy
            Point spot = _startSpots[unit.owner];
            Point bspot = bangobj.board.getOccupiableSpot(spot.x, spot.y, 3);
            if (bspot == null) {
                log.warning("Unable to locate spawn spot for to-be-respawned " +
                            "unit [unit=" + unit + ", spot=" + spot + "].");
                // stick him back on the queue for a few ticks later
                unit.setRespawnTick((short)(tick + RESPAWN_TICKS));
                _respawns.add(unit);
                continue;
            }

            // reset him, position him, and add him back in (if necessary)
            unit.damage = 0;
            unit.influence = null;
            unit.setRespawnTick((short)0);
            unit.owner = _startingOwners.get(unit.pieceId);

            if (bangobj.pieces.containsKey(unit.getKey())) {
                // clear the shadow at its old location
                bangobj.board.updateShadow(unit, null);
                unit.position(bspot.x, bspot.y);
                bangobj.updatePieces(unit);
            } else {
                unit.position(bspot.x, bspot.y);
                bangobj.addToPieces(unit);
            }

            // shadow the unit at its new location
            bangobj.board.updateShadow(null, unit);
        }

        return false;
    }

    /**
     * Called when a unit makes a move in the game but before the
     * associated update for that unit is broadcast. The scenario can make
     * further adjustments to the unit and modify other game data as
     * appropriate.
     */
    public void unitMoved (BangObject bangobj, Unit unit)
    {
    }

    /**
     * Called when a piece was killed. The scenario can choose to respawn
     * the piece later, and do whatever else is appropriate.
     */
    public void pieceWasKilled (BangObject bangobj, Piece piece)
    {
        if (respawnPieces()) {
            maybeQueueForRespawn(piece, bangobj.tick);
        }
    }

    /**
     * If a scenario wishes for pieces to respawn, it should override this
     * method and return true.
     */
    protected boolean respawnPieces ()
    {
        return false;
    }

    /**
     * Called when a piece "dies" to potentially queue it up for
     * respawning (assuming it's a unit and that it's marked for respawn.
     */
    protected void maybeQueueForRespawn (Piece piece, short tick)
    {
        if (!_startingOwners.contains(piece.pieceId) ||
            !(piece instanceof Unit)) {
            return;
        }
        Unit unit = (Unit)piece;
        unit.setRespawnTick((short)(tick + RESPAWN_TICKS));
        _respawns.add(unit);
        log.info("Queued for respawn " + unit + ".");
    }

    /** Used to track the locations where players are started. */
    protected Point[] _startSpots;

    /** Used to note the starting owner for the starting pieces. */
    protected IntIntMap _startingOwners = new IntIntMap();

    /** A list of units waiting to be respawned. */
    protected ArrayList<Unit> _respawns = new ArrayList<Unit>();

    /** The number of ticks that must elapse before a unit is respawned. */
    protected static final int RESPAWN_TICKS = 12;
}
