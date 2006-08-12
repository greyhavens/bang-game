//
// $Id$

package com.threerings.bang.game.server.ai;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.server.BangManager;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PointSet;

/**
 * Handles the logic for a single non-aligned AI piece.
 */
public abstract class PieceLogic
{
    /** Our piece ID. */
    public int pieceId;

    /**
     * Initializes the AI logic when the piece is created.
     */
    public void init (BangManager bangmgr, Piece piece)
    {
        _bangmgr = bangmgr;
        _bangobj = (BangObject)_bangmgr.getPlaceObject();
        pieceId = piece.pieceId;
    }

    /**
     * Called on every tick to let the AI move the piece.
     */
    public void tick (Piece[] pieces, short tick)
    {
        for (Piece p : pieces) {
            if (p.pieceId == pieceId && p.isAlive() &&
                    p.ticksUntilMovable(tick) == 0) {
                Unit unit = (Unit)p;
                _moves.clear();
                _attacks.clear();
                unit.computeMoves(_bangobj.board, _moves, _attacks);
                moveUnit(pieces, unit, _moves, _attacks);
            }
        }
    }

    /**
     * Moves an owned, ticked-up unit.
     *
     * @param pieces the array of pieces on the board
     * @param unit the unit to move
     * @param moves the places to which the unit can move
     * @param attacks the places the unit can attack
     */
    protected abstract void moveUnit (
        Piece[] pieces, Unit unit, PointSet moves, PointSet attacks);

    /**
     * Orders a unit to move.
     *
     * @param unit the unit to move
     * @param x the x coordinate to which to move or {@link Short#MAX_VALUE} if
     * the unit should be moved to the closest valid firing position to the
     * target.
     * @param y the y coordinate to which to move, this is ignored if {@link
     * Short#MAX_VALUE} is supplied for x.
     * @param target the piece for the unit to shoot, or <code>null</code> for
     * none
     * @return true if the order was executed, false if there was some error in
     * executing the order
     */
    protected boolean executeOrder (Unit unit, int x, int y, Piece target)
    {
        try {
            int targetId = (target == null) ? -1 : target.pieceId;
            _bangmgr.executeOrder(unit, x, y, targetId, true);
            return true;
        } catch (InvocationException e) {
            return false;
        }
    }

    /** Reference to the Bang Manager. */
    protected BangManager _bangmgr;

    /** Reference to the game object. */
    protected BangObject _bangobj;

    /** Used to compute a unit's potential moves or attacks. */
    protected PointSet _moves = new PointSet(), _attacks = new PointSet();
}
