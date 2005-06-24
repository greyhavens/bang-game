//
// $Id$

package com.threerings.bang.util;

import java.util.ArrayList;
import java.util.Iterator;

import com.threerings.bang.data.piece.Piece;

/**
 * Utility methods relating to pieces.
 */
public class PieceUtil
{
    /**
     * Returns a list of the pieces in the supplied collection that
     * overlap the supplied piece or null if no pieces overlap.
     */
    public static ArrayList<Piece> getOverlappers (Iterator iter, Piece piece)
    {
        ArrayList<Piece> lappers = null;
        while (iter.hasNext()) {
            Piece p = (Piece)iter.next();
            if (p.pieceId != piece.pieceId && p.intersects(piece)) {
                if (lappers == null) {
                    lappers = new ArrayList<Piece>();
                }
                lappers.add(p);
            }
        }
        return lappers;
    }
}
