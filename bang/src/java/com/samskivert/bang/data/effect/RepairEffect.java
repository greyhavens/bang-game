//
// $Id$

package com.samskivert.bang.data.effect;

import java.util.ArrayList;

import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.util.PieceSet;

/**
 * Does something extraordinary.
 */
public class RepairEffect extends Effect
{
    public int pieceId;

    public RepairEffect (int pieceId)
    {
        this.pieceId = pieceId;
    }

    public RepairEffect ()
    {
    }

    public void apply (BangObject bangobj, ArrayList<Piece> additions,
                       PieceSet removals)
    {
        Piece piece = (Piece)bangobj.pieces.get(pieceId);
        if (piece != null) {
            piece.damage = 0;
        }
    }
}
