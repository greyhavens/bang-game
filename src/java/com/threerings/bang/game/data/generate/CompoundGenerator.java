//
// $Id$

package com.threerings.bang.game.data.generate;

import java.awt.Rectangle;
import java.util.ArrayList;

import com.samskivert.util.IntTuple;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.BigPiece;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;

/**
 * Scatters props haphazardly around to create a random board.
 */
public class CompoundGenerator extends EnvironmentGenerator
{
    @Override // documentation inherited
    public void generate (BangBoard board, ArrayList<Piece> pieces)
    {
        int width = board.getWidth() - BangBoard.BORDER_SIZE*2,
            height = board.getHeight() - BangBoard.BORDER_SIZE*2;

        int density = 11 - 5; // config.density;

        // pick a reasonable number for a board of this size (1/2 of the
        // average of the width and height)
        int count = 2*(width+height)/density;

        // TODO: maybe more props at higher difficulty

        // now scatter some props around; first place them randomly
        BigPiece[] bldgs = new BigPiece[count];
        for (int ii = 0; ii < count; ii++) {
            int rando = RandomUtil.getInt(100);
            if (rando > 95) {
                bldgs[ii] = Prop.getProp("buildings/saloon");
            } else if (rando > 65) {
                bldgs[ii] = Prop.getProp("buildings/saloon");
            } else {
                bldgs[ii] = Prop.getProp("buildings/saloon");
            }
            BigPiece bldg = bldgs[ii];

            // position it randomly
            int tx = RandomUtil.getInt(width-bldg.getBounds().width) + BangBoard.BORDER_SIZE;
            int ty = RandomUtil.getInt(height-bldg.getBounds().height) + BangBoard.BORDER_SIZE;
            bldg.position(tx, ty);

            // rotate it randomly
            switch (RandomUtil.getInt(100) / 25) {
            case 0: break;
            case 1: bldg.rotate(Piece.CW); break;
            case 2: bldg.rotate(Piece.CCW); break;
            case 3:
                bldg.rotate(Piece.CCW);
                bldg.rotate(Piece.CCW);
                break;
            }
        }

        // then move them around a bit to resolve overlaps and ensure
        // sufficient space between each prop (at least room for an ant)
        for (int cc = 0; cc < 10*count; cc++) {
            int adjusted = 0;
            for (int ii = 0; ii < count; ii++) {
                BigPiece bldg = bldgs[ii];
                BigPiece[] neighbors = new BigPiece[Piece.DIRECTIONS.length];

                // see how close we are to our neighbors
                for (int dd = 0; dd < neighbors.length; dd++) {
                    int direction = Piece.DIRECTIONS[dd];
                    BigPiece neigh = getClosest(bldgs, bldg, direction);
                    if (neigh == null || !tooClose(bldg, neigh)) {
                        continue;
                    }

                    // if we're bigger than our neighbor, adjust them
                    // rather than us
                    BigPiece abldg = bldg;
                    if (bldg.getBounds().width > neigh.getBounds().width) {
                        abldg = neigh;
                        neigh = bldg;
                    }

                    // try moving the piece away from its neighbor
                    int dist = (1 - getDistance(direction, abldg, neigh));
                    int dx = dist * Piece.DX[direction];
                    int dy = dist * Piece.DY[direction];
//                     log.info(bldg.pieceId + " is too close to " + neigh.pieceId + ". Adjusting"
//                              "dx", dx, "dy", dy);
                    abldg.position(abldg.x + dx, abldg.y + dy);
                    adjusted++;
                }
            }

            if (adjusted == 0) {
                break;
            }
        }

        // finally add all the props that are still fully on the board
        Rectangle rect = new Rectangle(BangBoard.BORDER_SIZE,
            BangBoard.BORDER_SIZE, width, height);
        for (int ii = 0; ii < count; ii++) {
            if (rect.contains(bldgs[ii].getBounds())) {
                pieces.add(bldgs[ii]);
            } else {
//                 log.info("A " + bldgs[ii].getClass().getName() + " ended up off the board.");
                bldgs[ii] = null;
            }
        }
    }

    /**
     * Returns the piece closest to the specified piece in the specified
     * direction assuming that a piece "overlaps" if its bounds plus one
     * tile overlap with the specified piece's in that direction.
     */
    protected BigPiece getClosest (
        BigPiece[] pieces, BigPiece piece, int direction)
    {
        IntTuple range = new IntTuple(), crange = new IntTuple();
        getRange(piece, direction, range);
        // expand the range around the target piece by one to cause pieces
        // that don't overlay but have no space between them to "match"
        range.left -= 1;
        range.right += 1;

        BigPiece closest = null;
        int cdist = Integer.MAX_VALUE;
        for (int ii = 0; ii < pieces.length; ii++) {
            BigPiece cpiece = pieces[ii];
            if (cpiece == piece) {
                continue;
            }
            getRange(cpiece, direction, crange);
            if (!rangesOverlap(range, crange)) {
                continue;
            }

            int dist = Integer.MAX_VALUE;
            switch (direction) {
            case Piece.NORTH:
                dist = piece.getBounds().y - cpiece.getBounds().y;
                break;
            case Piece.SOUTH:
                dist = cpiece.getBounds().y - piece.getBounds().y;
                break;
            case Piece.EAST:
                dist = cpiece.getBounds().x - piece.getBounds().x;
                break;
            case Piece.WEST:
                dist = piece.getBounds().x - cpiece.getBounds().x;
                break;
            }

            if (dist >= 0 && dist < cdist) {
                closest = cpiece;
                cdist = dist;
            }
        }

        return closest;
    }

    /**
     * Returns true if the two pieces in question do not have a gap of at
     * least one tile between them.
     */
    protected boolean tooClose (BigPiece one, BigPiece two)
    {
        Rectangle rect = new Rectangle(one.getBounds());
        rect.grow(1, 1);
        return rect.intersects(two.getBounds());
    }

    /**
     * Returns the distance between the two pieces in the specified
     * direction.
     */
    protected int getDistance (int direction, BigPiece source, BigPiece target)
    {
        switch (direction) {
        case Piece.NORTH:
            return getDistance(Piece.SOUTH, target, source);
        case Piece.EAST:
            return (target.getBounds().x - (source.getBounds().x +
                                            source.getBounds().width));
        case Piece.SOUTH:
            return (target.getBounds().y - (source.getBounds().y +
                                            source.getBounds().height));
        case Piece.WEST:
            return getDistance(Piece.EAST, target, source);
        }
        return 0;
    }

    protected void getRange (BigPiece piece, int direction, IntTuple range)
    {
        Rectangle tb = piece.getBounds();
        switch (direction) {
        case Piece.NORTH:
        case Piece.SOUTH:
            range.left = tb.x;
            range.right = range.left + tb.width - 1;
            break;
        case Piece.WEST:
        case Piece.EAST:
            range.left = tb.y;
            range.right = range.left + tb.height - 1;
            break;
        }
    }

    protected boolean rangesOverlap (IntTuple range1, IntTuple range2)
    {
        return (range1.right >=  range2.left) && (range1.left <= range2.right);
    }
}
