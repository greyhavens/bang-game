//
// $Id$

package com.threerings.bang.game.util;

import java.util.ArrayList;

import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;

/**
 * Utility methods relating to pieces.
 */
public class PieceUtil
    implements PieceCodes
{
    /**
     * Returns a list of the pieces in the supplied collection that
     * overlap the supplied piece or null if no pieces overlap.
     */
    public static ArrayList<Piece> getOverlappers (
        Iterable<Piece> pieces, Piece piece)
    {
        ArrayList<Piece> lappers = null;
        for (Piece p : pieces) {
            if (p.pieceId != piece.pieceId && p.intersects(piece) && 
                    !(p instanceof Marker)) {
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
    
    /**
     * Given a piece, its current fine rotation, and a fine amount by which to
     * rotate the piece, performs any necessary coarse rotations on the piece
     * and returns the new fine rotation.
     */
    public static byte rotateFine (Piece piece, byte forient, int amount)
    {
        int nforient = forient + amount;
        if (nforient < -128) {
            piece.rotate(CW);
            nforient += 256;
            
        } else if (nforient > 127) {
            piece.rotate(CCW);
            nforient -= 256;
        }
        return (byte)nforient;
    }
}
