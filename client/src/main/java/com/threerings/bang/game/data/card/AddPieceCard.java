//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;

/**
 * A card that puts a piece on the board in an occupiable spot.
 */
public abstract class AddPieceCard extends AreaCard
{
    @Override // documentation inherited
    public int getRadius ()
    {
        return 0;
    }

    @Override // documentation inherited
    public boolean isValidLocation (BangObject bangobj, int tx, int ty)
    {
        return bangobj.board.isOccupiable(tx, ty);
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        if (!bangobj.board.isOccupiable(coords[0], coords[1])) {
            return null;
        }
        Piece piece = createPiece();
        piece.assignPieceId(bangobj);
        piece.position(coords[0], coords[1]);
        return new AddPieceEffect(piece, getAddedEffect());
    }
    
    /**
     * Creates and returns the piece to place.
     */
    protected abstract Piece createPiece ();
    
    /**
     * Returns the effect to fire on the piece after addition, if any.
     */
    protected String getAddedEffect ()
    {
        return null;
    }
}
