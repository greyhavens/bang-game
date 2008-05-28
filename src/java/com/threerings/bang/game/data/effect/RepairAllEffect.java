//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Heals all the living pieces on the board.
 */
public class RepairAllEffect extends GlobalEffect
{
    /** The effect applied to the board. */
    public static final String FORGIVEN = "indian_post/forgiven";
    
    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        affectBoard(bangobj, FORGIVEN, false, obs);
        return super.apply(bangobj, obs);
    }
    
    @Override // documentation inherited
    public boolean validPiece (Piece piece)
    {
        return (piece.isTargetable() && piece instanceof Unit && 
                piece.isAlive() && piece.damage > 0);
    }

    @Override // documentation inherited
    public Effect getEffect (Piece piece)
    {
        return new RepairEffect(piece.pieceId);
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        return "m.effect_forgiven";
    }
}
