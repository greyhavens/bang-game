//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Adds a piece to the board.
 */
public class AddPieceEffect extends Effect
{
    /** The piece to add. */
    public Piece piece;
    
    public AddPieceEffect ()
    {
    }
    
    public AddPieceEffect (Piece piece)
    {
        this.piece = piece;
    }
    
    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { piece.pieceId };
    }
    
    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // make sure the location of the piece is still clear
        if (!bangobj.board.isOccupiable(piece.x, piece.y)) {
            piece = null;
            return;
        }
        piece.assignPieceId(bangobj);
        bangobj.board.shadowPiece(piece);
    }
    
    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return piece != null;
    }
    
    // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        bangobj.addPieceDirect(piece);
        reportAddition(obs, piece);
        return true;
    }
}
