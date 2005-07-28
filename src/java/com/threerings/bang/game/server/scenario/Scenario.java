//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;

import com.samskivert.util.IntIntMap;
import com.threerings.media.util.MathUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
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
     * Allows a scenario to filter out custom marker pieces prior to the
     * start of the round. <em>Note:</em> this is called before {@link
     * #init}.
     *
     * @param bangobj the game object.
     * @param starts a list of start markers for all the players.
     * @param pieces the remaining pieces on the board.
     */
    public void filterMarkers (BangObject bangobj, ArrayList<Piece> starts,
                               ArrayList<Piece> pieces)
    {
        // nothing to do by default
    }

    /**
     * Called when a round is about to start.
     *
     * @throws InvocationException containing a translatable string
     * indicating why the scenario is booched, which will be displayed to
     * the players and the game will be cancelled.
     */
    public void init (BangObject bangobj, ArrayList<Piece> starts,
                      PointSet bonusSpots, PieceSet purchases)
        throws InvocationException
    {
        // clear our respawn queue
        _respawns.clear();

        // this will contain the starting spot for each player
        _startSpots = new Point[bangobj.players.length];
        for (int ii = 0; ii < _startSpots.length; ii++) {
            Piece p = starts.get(ii);
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

            // reassign the unit to its original owner
            unit.owner = _startingOwners.get(unit.pieceId);

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
     * Called when a piece makes a move in the game but before the
     * associated update for that piece is broadcast. The scenario can
     * make further adjustments to the piece and modify other game data as
     * appropriate.
     *
     * @return null if nothing happens to this piece as a result of the
     * move or an effect to apply to the piece.
     */
    public Effect pieceMoved (BangObject bangobj, Piece piece)
    {
        return null;
    }

    /**
     * Called when a piece was killed. The scenario can choose to respawn
     * the piece later, and do whatever else is appropriate.
     *
     * @return true if the piece should be updated as a result of changes
     * made by the scenario.
     */
    public boolean pieceWasKilled (BangObject bangobj, Piece piece)
    {
        if (respawnPieces()) {
            maybeQueueForRespawn(piece, bangobj.tick);
        }
        return false;
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

    /**
     * Helper function useful when initializing scenarios. Determines the
     * player whose start marker is closest to the specified piece and is
     * therefore the <em>owner</em> of that piece.
     *
     * @return -1 if no start markers exist at all or the player index of
     * the closest marker.
     */
    protected int getOwner (Piece target, ArrayList<Piece> starts)
    {
        int mindist2 = Integer.MAX_VALUE, idx = -1;
        for (int ii = 0, ll = starts.size(); ii < ll; ii++) {
            Piece start = starts.get(ii);
            int dist2 = MathUtil.distanceSq(
                target.x, target.y, start.x, start.y);
            if (dist2 < mindist2) {
                mindist2 = dist2;
                idx = ii;
            }
        }
        return idx;
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
