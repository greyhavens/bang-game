//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;
import java.awt.Point;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.util.PointSet;

/**
 * Special handling for the nugget bonus.
 */
public class Nugget extends Bonus
{
    @Override // documentation inherited
    public Point getDropLocation (BangObject bangobj)
    {
        // try not to drop a nugger next to a claim
        ArrayList<Point> points = bangobj.board.getOccupiableSpots(
                50, x, y, 3);
        PointSet reserved = new PointSet();
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Counter) {
                for (int ii = 0; ii < DIRECTIONS.length; ii++) {
                    reserved.add(piece.x + DX[ii], piece.y + DY[ii]);
                }
            }
        }
        for (Point pt : points) {
            if (!reserved.contains(pt.x, pt.y)) {
                return pt;
            }
        }
        return (points.size() > 0 ? points.get(0) : null);
    }
}
