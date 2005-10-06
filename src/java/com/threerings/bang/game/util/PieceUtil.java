//
// $Id$

package com.threerings.bang.game.util;

import java.util.ArrayList;
import java.util.Iterator;

import com.threerings.bang.game.data.piece.Piece;

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

    /**
     * Returns the direction piece a should be oriented to "face" piece b.
     */
    public static short getDirection (Piece a, Piece b)
    {
        // determine the angle between the two pieces
        double theta = Math.atan2(b.y-a.y, b.x-a.x);
        // translate and scale that angle such that NORTH is 0, EAST is 1,
        // SOUTH is 2 and WEST is -1
        double stheta = ((theta + Math.PI/2) * 2) / Math.PI;
        // then round and modulate
        return (short)(((int)Math.round(stheta) + 4) % 4);
    }
}
