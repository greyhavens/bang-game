//
// $Id$

package com.samskivert.bang.data.piece;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.RandomUtil;

import com.threerings.presents.dobj.DSet;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.data.BangBoard;
import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.Terrain;
import com.samskivert.bang.util.PieceSet;
import com.samskivert.bang.util.PointSet;

import static com.samskivert.bang.Log.log;

/**
 * Does something extraordinary.
 */
public abstract class Piece extends SimpleStreamableObject
    implements Cloneable, DSet.Entry, PieceCodes
{
    /** Used by {@link #maybeInteract}. */
    public enum Interaction { CONSUMED, ENTERED, INTERACTED, NOTHING };

    /** Uniquely identifies each piece in the game. */
    public int pieceId;

    /** The player index of the owner of this piece or -1 if it is not an
     * owned piece. */
    public int owner = -1;

    /** The current x location of this piece's segments. */
    public short x;

    /** The current y location of this piece's segments. */
    public short y;

    /** This piece's orientation. */
    public short orientation;

    /** The energy level of this piece. */
    public int energy;

    /** -1 if this piece is not on a path or the index into their path if
     * they are. */
    public int pathPos = -1;

    /**
     * Returns true if this piece is on a path.
     */
    public boolean hasPath ()
    {
        return pathPos >= 0;
    }

    /**
     * Returns true if the specified coordinates intersect this piece.
     */
    public boolean intersects (int tx, int ty)
    {
        return (x == tx) && (y == ty);
    }

    /**
     * Returns true if this piece intersects the specified region.
     */
    public boolean intersects (Rectangle bounds)
    {
        return bounds.contains(x, y);
    }

    /**
     * Returns true if these two pieces intersect at their current
     * coordinates.
     */
    public boolean intersects (Piece other)
    {
        return other.intersects(x, y);
    }

    /** Returns the width of this piece. */
    public int getWidth ()
    {
        return 1;
    }

    /** Returns the height of this piece. */
    public int getHeight ()
    {
        return 1;
    }

    /**
     * Allows the piece to do any necessary initialization before the game
     * starts.
     */
    public void init ()
    {
        // set up our starting energy (only if it hasn't been otherwise
        // configured in the editor)
        if (energy == 0) {
            energy = startingEnergy();
        }
    }

    /**
     * Updates this pieces position and orientation.
     *
     * @return true if the piece's position changed, false if not.
     */
    public boolean position (int nx, int ny)
    {
        // avoid NOOP
        if (nx != x || ny != y) {
            updatePosition(nx, ny);
            pieceMoved();
            return true;
        }
        return false;
    }

    /**
     * Instructs the piece to rotate clockwise if direction is {@link Piece#CW}
     * and counter-clockwise if it is {@link Piece#CCW}.
     *
     * @return true if rotation is supported and the piece rotated, false
     * if it is not supported (pieces longer than one segment cannot be
     * rotated).
     */
    public boolean rotate (int direction)
    {
        // update our orientation
        orientation = (short)((direction == CW) ? ((orientation + 1) % 4) :
                              ((orientation + 3) % 4));

        // let derived classes know that we've moved
        pieceMoved();

        return true;
    }

    /**
     * Returns true if this piece is a flying piece, false if it is a
     * walking piece.
     */
    public boolean isFlyer ()
    {
        return false;
    }

    /**
     * Returns the energy consumed per step taken by this piece.
     */
    public int energyPerStep ()
    {
        return DEFAULT_ENERGY_PER_STEP;
    }

    /**
     * Computes and returns this piece's locus of attention, which is
     * abstractly the spot between their eyes (in floating point
     * coordinates). For a segmented piece, this is approximated as the
     * center of the tile that contains the head.
     */
    public Point2D getLocusOfAttention ()
    {
        _locus.setLocation(x + 0.5, y + 0.5);
        return _locus;
    }

    /**
     * Returns the orientation we should face to orient toward the
     * specified piece.
     */
    public short computeOrientOrient (Piece other)
    {
        Point2D loc = getLocusOfAttention();
        Point2D oloc = other.getLocusOfAttention();
        double theta = Math.atan2(oloc.getX() - loc.getX(),
                                  oloc.getY() - loc.getY());
        log.info("orienting " + loc + " -> " + oloc + " = " + theta);
        // atan2() returns a value from -PI to PI, so we add PI to shift
        // if from 0 to PI and we add PI/4 to shift it a bit further such
        // that 0 is the start of a NORTH orientation
        theta += 5 * Math.PI / 4;
        // then we just scale and truncate
        short orient = (short)Math.floor((4 * theta / Math.PI) % 4);
        return orient;
    }

    /**
     * Instructs this piece to consume the energy needed to take the
     * specified number of steps.
     */
    public void consumeEnergy (int steps)
    {
        energy -= energyPerStep() * steps;
    }

    /**
     * Returns true if this piece has at least enough energy to take one
     * step, false if not.
     */
    public boolean canTakeStep ()
    {
        return energy >= energyPerStep();
    }

    /**
     * Returns true if this piece prevents other pieces from occupying the
     * same square, or false if it can colocate.
     */
    public boolean preventsOverlap (Piece lapper)
    {
        return true;
    }

    /**
     * Some pieces interact with other pieces, which takes place via this
     * method. Depending on the type of interaction, the piece can
     * indicate that it consumed the other piece, was consumed by it
     * (entered), simply interacted with it resulting in both pieces being
     * changed or did nothing at all.
     */
    public Interaction maybeInteract (Piece other)
    {
        if (other instanceof Fuel && energy < 3*maximumEnergy()/4) {
            Fuel nibbly = (Fuel)other;
            int taken = nibbly.takeEnergy(this);
            energy = Math.min(maximumEnergy(), energy + taken);
            return nibbly.energy > 0 ?
                Interaction.INTERACTED : Interaction.CONSUMED;
        }
        return Interaction.NOTHING;
    }

    /**
     * Verifies that a move to the specified location is within the
     * piece's capabilities (ie. not too far, doesn't turn illegally,
     * doesn't cross illegal tiles).
     */
    public boolean canMoveTo (BangBoard board, int nx, int ny)
    {
        // by default, ensure that the location is exactly one unit away
        // from our current location
        if (Math.abs(x - nx) + Math.abs(y - ny) != 1) {
            return false;
        }

        // and make sure we can traverse our final location
        return canTraverse(board, nx, ny);
    }

    /**
     * Returns true if this bug can traverse the board at the specified
     * coordinates.
     */
    public boolean canTraverse (BangBoard board, int tx, int ty)
    {
        // by default, we assume that our tail always follows our head and
        // moves to tiles we already occupied, so we only need to
        // determine whether our head is moving onto a traversable tile
        return canTraverse(board.getTile(tx, ty));
    }

    /**
     * Allows a piece to modify the board terrain as a result of landing
     * on it.
     *
     * @return {@link Terrain#NONE} if the piece does not wish to modify
     * the terrain, or the terrain code for the new terrain type if it
     * does.
     */
    public Terrain modifyBoard (BangBoard board, int tx, int ty)
    {
        return Terrain.NONE;
    }

    /**
     * Allows this piece to react to the state of the board at the
     * termination of the previous turn. It should add itself and any
     * other modified pieces to the updates set (assuming it or another
     * piece changes as a result of the reaction).
     *
     * <em>Note:<em> it is legal for a piece to remove another piece from
     * the board as a result of its reaction. In that case, it should
     * effect the appropriate removal from the supplied game object
     * directly.
     */
    public void react (BangObject bangobj, Piece[] pieces, PieceSet updates)
    {
    }

    /**
     * Enumerates the coordinates of the legal moves for this piece, given
     * the specified starting location. These moves need not account for
     * terrain or other potential blockage.
     */
    public void enumerateLegalMoves (int tx, int ty, PointSet moves)
    {
        // the default piece can move one in any of the four cardinal
        // directions
        moves.add(tx+1, ty);
        moves.add(tx-1, ty);
        moves.add(tx, ty+1);
        moves.add(tx, ty-1);
    }

    /**
     * Enumerates the coordinates of the tiles that this piece can attack
     * from its current location.
     */
    public void enumerateAttacks (PointSet set)
    {
        // by default, none
    }

    /**
     * Enumerates the coordinates of the tiles to which this piece attends
     * and may response if another piece moves into one of those spaces.
     */
    public void enumerateAttention (PointSet set)
    {
        // by default, none
    }

    /**
     * Creates the appropriate derivation of {@link PieceSprite} to render
     * this piece.
     */
    public PieceSprite createSprite ()
    {
        return new PieceSprite();
    }

    /**
     * This is normally not needed, but is used by the editor to assign
     * piece IDs to new pieces.
     */
    public void assignPieceId ()
    {
        _key = null;
        pieceId = 0;
        getKey();
    }

    /** Returns the percentage remaining of this piece's energy. */
    public int getPercentEnergy ()
    {
        return energy * 100 / maximumEnergy();
    }

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        if (_key == null) {
            if (pieceId == 0) {
                pieceId = ++_nextPieceId;
            }
            _key = new Integer(pieceId);
        }
        return _key;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return pieceId;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return pieceId == ((Piece)other).pieceId;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return (Piece)super.clone();
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }

    /** Converts our orientation to a human readable string. */
    public String orientationToString ()
    {
        return (orientation >= 0) ? ORIENT_CODES[orientation] :
            ("" + orientation);
    }

    /**
     * Computes the new orientation for this piece were it to travel from
     * its current coordinates to the specified coordinates.
     */
    protected int computeOrientation (int nx, int ny)
    {
        int hx = x, hy = y;

        // if it is purely a horizontal or vertical move, simply orient
        // in the direction of the move
        if (nx == hx) {
            return (ny > hy) ? SOUTH : NORTH;
        } else if (ny == hy) {
            return (nx > hx) ? EAST : WEST;
        }

        // otherwise try to behave naturally: moving forward first if
        // possible and turning sensibly to reach locations behind us
        switch (orientation) {
        case NORTH: return (ny < hy) ? ((nx > hx) ? EAST : WEST) : SOUTH;
        case SOUTH: return (ny > hy) ? ((nx > hx) ? EAST : WEST) : NORTH;
        case EAST:  return (nx > hx) ? ((ny > hy) ? SOUTH : NORTH) : WEST;
        case WEST:  return (nx < hx) ? ((ny > hy) ? SOUTH : NORTH) : EAST;
        // erm, this shouldn't happen
        default: return NORTH;
        }
    }

    /**
     * Called by {@link #position} after it has confirmed that we are in
     * fact changing position and not NOOPing or setting our location for
     * the first time. Derived pieces that want to customize their
     * position handling should override this method.
     */
    protected void updatePosition (int nx, int ny)
    {
        // determine our new orientation
        orientation = (short)computeOrientation(nx, ny);
        x = (short)nx;
        y = (short)ny;
    }

    /**
     * Called after this piece changes position or orientation.
     */
    protected void pieceMoved ()
    {
    }

    /**
     * Returns true if this piece can traverse the specified type of
     * terrain.
     */
    protected boolean canTraverse (Terrain terrain)
    {
        return (terrain == Terrain.DIRT ||
                terrain == Terrain.LEAF_BRIDGE);
    }

    /** Returns the starting energy for pieces of this type. */
    protected int startingEnergy ()
    {
        return DEFAULT_STARTING_ENERGY * 10;
    }

    /** Returns the maximum energy this piece can possess. */
    protected int maximumEnergy ()
    {
        return DEFAULT_MAXIMUM_ENERGY * 10;
    }

    /**
     * Returns a random piece from the array of supplied pieces that both
     * matches the piece predicate and intersects any point in the
     * supplied point set. Returns null if no matching pieces so
     * intersect.
     */
    protected Piece checkSet (PointSet set, Piece[] pieces, PiecePredicate pred)
    {
        // determine our locus of attention for distance computations
        double closest = Double.MAX_VALUE;
        Point2D loc = getLocusOfAttention();

        ArrayList<Piece> matches = null;
        for (int pp = 0; pp < pieces.length; pp++) {
            Piece piece = pieces[pp];
            if (!pred.matches(piece)) {
                continue;
            }

            // determine if this piece is close enough
            double dist = loc.distance(piece.getLocusOfAttention());
            if (dist > closest) {
                continue;
            }

            for (int ii = 0, ll = set.size(); ii < ll; ii++) {
                int tx = set.getX(ii), ty = set.getY(ii);
                if (piece.intersects(tx, ty)) {
                    if (matches == null) {
                        matches = new ArrayList<Piece>();
                    }
                    if (dist < closest) {
                        matches.clear();
                    }
                    matches.add(piece);
                }
            }
        }
        return matches == null ? null : (Piece)RandomUtil.pickRandom(matches);
    }

    /**
     * Computes an attack and attend set for this piece given the supplied
     * definition matrix and piece size.
     */
    protected void computeSets (int[] matrix, int msize, int psize,
                                PointSet attack, PointSet attend)
    {
        // compute our translation offsets
        int offx = x - (msize-psize)/2;
        int offy = y - (msize-psize)/2;

        // foreach each element of our sets, properly translate and rotate
        // the coordinate and add it to the appropriate set
        for (int xx = 0; xx < msize; xx++) {
            for (int yy = 0; yy < msize; yy++) {
                int tt = matrix[yy * msize + xx];
                PointSet set = null;
                if (tt == 1) {
                    set = attend;
                } else if (tt == 2) {
                    set = attack;
                } else {
                    continue;
                }

                // "rotate" the matrix according to our orientation
                int tx = xx, ty = yy;
                switch (orientation) {
                case EAST:
                    tx = (msize-1-yy);
                    ty = xx;
                    break;
                case SOUTH:
                    tx = (msize-1-xx);
                    ty = (msize-1-yy);
                    break;
                case WEST:
                    tx = yy;
                    ty = (msize-1-xx);
                    break;
                }

                // then translate it and add it to the set
                set.add(offx+tx, offy+ty);
            }
        }
    }

    public static void main (String[] args)
    {
        System.out.println("Theta: " + Math.atan2(0, 1));
        System.out.println("Theta: " + Math.atan2(1, 1));
        System.out.println("Theta: " + Math.atan2(1, 0));
        System.out.println("Theta: " + Math.atan2(1, -1));
        System.out.println("Theta: " + Math.atan2(0, -1));
        System.out.println("Theta: " + Math.atan2(-1, -1));
        System.out.println("Theta: " + Math.atan2(-1, 0));
        System.out.println("Theta: " + Math.atan2(-1, 1));
    }

    /** Used by {@link #checkSet}. */
    protected static interface PiecePredicate
    {
        public boolean matches (Piece piece);
    }

    protected transient Integer _key;
    protected transient Point2D _locus = new Point2D.Double();

    protected static int _nextPieceId;

    /** The default quantity of energy consumed to take a step. */
    protected static final int DEFAULT_ENERGY_PER_STEP = 10;

    /** The default starting quantity of energy. */
    protected static final int DEFAULT_STARTING_ENERGY = 100;

    /** The default maximum quantity of energy. */
    protected static final int DEFAULT_MAXIMUM_ENERGY = 250;

    /** Used to move one tile forward from an orientation. */
    protected static final int[] FWD_X_MAP = { 0, 1, 0, -1 };

    /** Used to move one tile forward from an orientation. */
    protected static final int[] FWD_Y_MAP = { -1, 0, 1, 0 };

    /** Used to move one tile backward from an orientation. */
    protected static final int[] REV_X_MAP = { 0, -1, 0, 1 };

    /** Used to move one tile backward from an orientation. */
    protected static final int[] REV_Y_MAP = { 1, 0, -1, 0 };

    /** Used by {@link #orientationToString}. */
    protected static final String[] ORIENT_CODES = { "N", "E", "S", "W" };
}
