//
// $Id$

package com.samskivert.bang.data.piece;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.client.sprite.UnitSprite;
import com.samskivert.bang.data.BangBoard;
import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.Shot;
import com.samskivert.bang.util.PieceSet;
import com.samskivert.bang.util.PointSet;

import static com.samskivert.bang.Log.log;

/**
 * Handles the state and behavior of the tank piece.
 */
public class Tank extends Piece
    implements PlayerPiece
{
    /** A tank can fire at a target up to two squares away. */
    public static final int FIRE_DISTANCE = 2;

    /** Indicates the orientation of our turret. */
    public short turretOrient;

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite("tank");
    }

    @Override // documentation inherited
    public boolean canBonusMove (int tx, int ty)
    {
        // if it's in the direction of motion, we can
        int nx = x + PieceCodes.DX[orientation];
        int ny = y + PieceCodes.DY[orientation];
        return (tx == nx && ty == ny);
    }

    @Override // documentation inherited
    public void react (BangObject bangobj, Piece[] pieces, PieceSet updates,
                       ArrayList<Shot> shots)
    {
        Piece[] targets = getTargets(pieces);

        // if there is an enemy piece in our sights, shoot it
        Piece target = targets[turretOrient];
        if (validTarget(target)) {
            shots.add(shoot(target));
            return;
        }

        // see if we can rotate our turret one notch and fire on a piece
        int ccw = (turretOrient + 3) % 4, cw = (turretOrient + 1) % 4, dir;
        if (validTarget(target = targets[dir = ccw]) ||
            validTarget(target = targets[dir = cw])) {
            turretOrient = (short)dir;
            updates.add(this);
            shots.add(shoot(target));
            return;
        }

        // rotate our turrent toward the direction of motion
        if (turretOrient == orientation) {
            return;
        }
        if (ccw == orientation) {
            turretOrient = (short)ccw;
        } else if (cw == orientation) {
            turretOrient = (short)cw;
        } else {
            turretOrient = (short)cw;
        }
        updates.add(this);
    }

    @Override // documentation inherited
    public void enumerateLegalMoves (int tx, int ty, PointSet moves)
    {
        moves.add(tx, ty-2);
        moves.add(tx, ty-1);

        moves.add(tx+2, ty);
        moves.add(tx+1, ty);
        moves.add(tx-1, ty);
        moves.add(tx-2, ty);

        moves.add(tx, ty+1);
        moves.add(tx, ty+2);
    }

    @Override // documentation inherited
    public void enumerateAttacks (PointSet set)
    {
        for (int xx = x - FIRE_DISTANCE; xx <= x + FIRE_DISTANCE; xx++) {
            if (xx != x) {
                set.add(xx, y);
            }
        }
        for (int yy = y - FIRE_DISTANCE; yy <= y + FIRE_DISTANCE; yy++) {
            if (yy != y) {
                set.add(x, yy);
            }
        }
    }

    @Override // documentation inherited
    public boolean canMoveTo (BangBoard board, int nx, int ny)
    {
        // we can move up to two squares in a turn
        if (Math.abs(x - nx) + Math.abs(y - ny) > 2) {
            return false;
        }

        // and make sure we can traverse our final location
        return canTraverse(board, nx, ny);
    }

    /**
     * Locates the nearest targets in each of the four directions.
     */
    public Piece[] getTargets (Piece[] pieces)
    {
        Arrays.fill(_closest, Integer.MAX_VALUE);
        Arrays.fill(_targets, null);

        for (int ii = 0; ii < pieces.length; ii++) {
            Piece piece = pieces[ii];
            if (piece == this) {
                continue;
            }

            // determine if this piece is within FIRE_DISTANCE of our
            // coordinate and on the same axis
            int dist = Integer.MAX_VALUE, index = -1;
            if (piece.x == x) {
                dist = Math.abs(piece.y - y);
                if (dist <= FIRE_DISTANCE) {
                    index = (piece.y < y) ? NORTH : SOUTH;
                }
            } else if (piece.y == y) {
                dist = Math.abs(piece.x - x);
                if (dist <= FIRE_DISTANCE) {
                    index = (piece.x < x) ? WEST : EAST;
                }
            }

            if (index != -1 && dist < _closest[index]) {
                _targets[index] = piece;
                _closest[index] = dist;
            }
        }

        return _targets;
    }

    /** Converts our orientation to a human readable string. */
    public String turretOrientToString ()
    {
        return ORIENT_CODES[turretOrient];
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        if (target instanceof Tank) {
            return 25;
        } else if (target instanceof Chopper) {
            return 25;
        } else if (target instanceof Artillery) {
            return 25;
        } else if (target instanceof Marine) {
            return 50;
        } else {
            return super.computeDamage(target);
        }
    }

    /** Used by {@link #getTargets}. */
    protected static int[] _closest = new int[4];

    /** Used by {@link #getTargets}. */
    protected static Piece[] _targets = new Piece[4];
}
