//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

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

    @Override // documentation inherited
    public Rectangle getBounds ()
    {
        return new Rectangle(piece.x, piece.x, 1, 1);
    }
    
    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        bangobj.board.shadowPiece(piece);
    }
    
    // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        addAndReport(bangobj, piece, obs);
        return true;
    }
}
