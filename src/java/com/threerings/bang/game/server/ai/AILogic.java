//
// $Id$

package com.threerings.bang.game.server.ai;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.util.PointSet;

/**
 * Handles the logic for a single AI player in a scenario.
 */
public abstract class AILogic
{
    /**
     * Initializes the AI logic before the start of a round.
     */
    public void init (BangManager bangmgr, int pidx)
    {
        _bangmgr = bangmgr;
        _bangobj = (BangObject)_bangmgr.getPlaceObject();
        _pidx = pidx;
    }
    
    /**
     * Returns the type of Big Shot desired by the AI.
     */
    public abstract String getBigShotType ();
    
    /**
     * Returns the types of cards desired by the AI (or <code>null</code> for
     * no cards).
     */
    public abstract String[] getCardTypes ();
    
    /**
     * Returns the types of units that the AI wants for its team.
     *
     * @param count the number of units allowed
     */
    public abstract String[] getUnitTypes (int count);
    
    /**
     * Called on every tick to let the AI move its pieces.  Default
     * implementation calls {@link #moveUnit} for each unit owned by the
     * AI that is ready to move.
     *
     * @param pieces the array of pieces on the board
     * @param tick the current tick
     */
    public void tick (Piece[] pieces, short tick)
    {
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof Unit && pieces[ii].owner == _pidx &&
                pieces[ii].isAlive() &&
                pieces[ii].ticksUntilMovable(tick) == 0) {
                Unit unit = (Unit)pieces[ii];
                _moves.clear();
                _attacks.clear();
                _bangobj.board.computeMoves(unit, _moves, _attacks);
                moveUnit(pieces, tick, unit, _moves, _attacks);
            }
        }
    }
    
    /**
     * Moves an owned, ticked-up unit.
     *
     * @param pieces the array of pieces on the board
     * @param tick the current tick
     * @param unit the unit to move
     * @param moves the places to which the unit can move
     * @param attacks the places the unit can attack
     */
    protected void moveUnit (
        Piece[] pieces, short tick, Unit unit, PointSet moves,
        PointSet attacks)
    {
    }
    
    /**
     * Orders a unit to move.
     *
     * @param unit the unit to move
     * @param x the x coordinate to which to move or {@link Short#MAX_VALUE} if
     * the unit should be moved to the closest valid firing position to the
     * target.
     * @param y the y coordinate to which to move, this is ignored if {@link
     * Short#MAX_VALUE} is supplied for x.
     * @param target the piece for the unit to shoot, or <code>null</code> for none
     * @return true if the order was executed, false if there was some error in
     * executing the order
     */
    protected boolean executeOrder (Unit unit, int x, int y, Piece target)
    {
        try {
            _bangmgr.executeOrder(unit, x, y, target, true);
            return true;
            
        } catch (InvocationException e) {
            return false;
        }
    }
    
    /** The game manager. */
    protected BangManager _bangmgr;
    
    /** The game object. */
    protected BangObject _bangobj;
    
    /** The index of the AI player. */
    protected int _pidx;
    
    /** Used to compute a unit's potential moves or attacks. */
    protected PointSet _moves = new PointSet(), _attacks = new PointSet();
}
