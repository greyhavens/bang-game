//
// $Id$

package com.samskivert.bang.data.piece;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.client.sprite.TankSprite;
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
    /** A tank can fire at a target up to four squares away. */
    public static final int FIRE_DISTANCE = 2;

    /** Indicates the orientation of our turret. */
    public short turretOrient;

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TankSprite();
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
            updates.add(target);
            return;
        }

        // see if we can rotate our turret one notch and fire on a piece
        int ccw = (turretOrient + 3) % 4, cw = (turretOrient + 1) % 4, dir;
        if (validTarget(target = targets[dir = ccw]) ||
            validTarget(target = targets[dir = cw])) {
            turretOrient = (short)dir;
            updates.add(this);
            shots.add(shoot(target));
            updates.add(target);
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
        moves.add(tx-1, ty-1);
        moves.add(tx, ty-1);
        moves.add(tx+1, ty-1);

        moves.add(tx+2, ty);
        moves.add(tx+1, ty);
        moves.add(tx-1, ty);
        moves.add(tx-2, ty);

        moves.add(tx-1, ty+1);
        moves.add(tx, ty+1);
        moves.add(tx+1, ty+1);
        moves.add(tx, ty+2);
    }

    @Override // documentation inherited
    public void enumerateAttacks (PointSet set)
    {
        for (int xx = x - 4; xx <= x + 4; xx++) {
            if (xx != x) {
                set.add(xx, y);
            }
        }
        for (int yy = y - 4; yy <= y + 4; yy++) {
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
        // configure our target regions
        _trects[NORTH].setLocation(x, y-FIRE_DISTANCE-1);
        _trects[EAST].setLocation(x+1, y);
        _trects[SOUTH].setLocation(x, y+1);
        _trects[WEST].setLocation(x-FIRE_DISTANCE-1, y);

        Arrays.fill(_closest, Integer.MAX_VALUE);
        Arrays.fill(_targets, null);

        for (int ii = 0; ii < pieces.length; ii++) {
            Piece piece = pieces[ii];
            if (piece == this) {
                continue;
            }

            // determine whether this piece intersects any of our target
            // regions and if so, if it is closer than our existing
            // closest match
            int dist = Integer.MAX_VALUE, index = -1;
            if (piece.intersects(_trects[NORTH])) {
                dist = y - piece.y + piece.getHeight();
                index = NORTH;
            } else if (piece.intersects(_trects[EAST])) {
                dist = piece.x - x;
                index = EAST;
            } else if (piece.intersects(_trects[SOUTH])) {
                dist = y - piece.y;
                index = SOUTH;
            } else if (piece.intersects(_trects[WEST])) {
                dist = x - piece.x;
                index = WEST;
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

    /**
     * Affects the target piece with damage.
     */
    public Shot shoot (Piece target)
    {
        int damage = Math.min(target.energy, target.maximumEnergy()/10);
        log.info("Doing " + damage + " damage to " + target + ".");
        target.energy -= damage;
        return new Shot(pieceId, target.x, target.y);
    }

    /** Returns true if we can and should fire upon this target. */
    protected boolean validTarget (Piece target)
    {
        return (target != null && target.owner != -1 &&
                target.owner != owner && target.energy > 0);
    }

    /** Used by {@link #getTargets}. */
    protected static Rectangle[] _trects = new Rectangle[] {
        new Rectangle(0, 0, 1, FIRE_DISTANCE), // NORTH
        new Rectangle(0, 0, FIRE_DISTANCE, 1), // EAST
        new Rectangle(0, 0, 1, FIRE_DISTANCE), // SOUTH
        new Rectangle(0, 0, FIRE_DISTANCE, 1) }; // WEST

    /** Used by {@link #getTargets}. */
    protected static int[] _closest = new int[4];

    /** Used by {@link #getTargets}. */
    protected static Piece[] _targets = new Piece[4];
}
