//
// $Id$

package com.samskivert.bang.data.effect;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;

import com.threerings.util.RandomUtil;

import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.util.PieceSet;

import static com.samskivert.bang.Log.log;

/**
 * Duplicates a piece.
 */
public class DuplicateEffect extends Effect
{
    public int pieceId;

    public DuplicateEffect (int pieceId)
    {
        this.pieceId = pieceId;
    }

    public DuplicateEffect ()
    {
    }

    public void apply (BangObject bangobj, ArrayList<Piece> additions,
                       PieceSet removals)
    {
        Piece piece = (Piece)bangobj.pieces.get(pieceId);
        if (piece == null) {
            return;
        }

        // find a place to put our new piece
        ArrayList<Point> spots = new ArrayList<Point>();
        spots.add(new Point(piece.x - 1, piece.y));
        spots.add(new Point(piece.x + 1, piece.y));
        spots.add(new Point(piece.x, piece.y - 1));
        spots.add(new Point(piece.x, piece.y + 1));

        for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece bp = (Piece)iter.next();
            for (int ii = 0, ll = spots.size(); ii < ll; ii++) {
                Point spot = spots.get(ii);
                if (bp.intersects(spot.x, spot.y)) {
                    spots.remove(ii--);
                    ll--;
                }
            }
        }

        if (spots.size() == 0) {
            log.info("Dropped duplicate effect. No spots " +
                     "[piece=" + piece + "].");
            return;
        }

        Point spot = (Point)RandomUtil.pickRandom(spots);
        Piece clone = piece.duplicate();
        clone.position(spot.x, spot.y);
        additions.add(clone);
    }
}
