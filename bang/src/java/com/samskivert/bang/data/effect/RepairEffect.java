//
// $Id$

package com.samskivert.bang.data.effect;

import java.util.ArrayList;

import com.samskivert.bang.data.BangBoard;
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

    public void apply (BangBoard board, Piece[] pieces,
                       ArrayList<Piece> additions, PieceSet removals)
    {
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii].pieceId == pieceId) {
                pieces[ii].damage = 0;
            }
        }
    }
}
