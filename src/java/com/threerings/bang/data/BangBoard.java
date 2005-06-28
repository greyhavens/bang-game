//
// $Id$

package com.threerings.bang.data;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.samskivert.util.ArrayUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.media.util.AStarPathUtil;

import com.threerings.bang.data.piece.BigPiece;
import com.threerings.bang.data.piece.Bonus;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Describes the terrain of the game board.
 */
public class BangBoard extends SimpleStreamableObject
{
    /** The basic traversal cost. */
    public static final int BASE_TRAVERSAL = 10;

    /** Creates a board with the specified dimensions. */
    public BangBoard (int width, int height)
    {
        _width = width;
        _height = height;
        _tiles = new int[width*height];
        _btstate = new byte[width*height];
        _estate = new byte[width*height];
        _tstate = new byte[width*height];
        _pgrid = new byte[width*height];
        _bbounds = new Rectangle(0, 0, _width, _height);
        fill(Terrain.NONE);
    }

    /** A default constructor for unserialization. */
    public BangBoard ()
    {
    }

    /** Returns the width of the board. */
    public int getWidth ()
    {
        return _width;
    }

    /** Returns the height of the board. */
    public int getHeight ()
    {
        return _height;
    }

    /**
     * Returns the bounds of our board. <em>Do not modify</em> the
     * returned rectangle.
     */
    public Rectangle getBounds ()
    {
        return _bbounds;
    }

    /** Fills the board with the specified tile. */
    public void fill (Terrain tile)
    {
        Arrays.fill(_tiles, tile.code);
    }

    /**
     * Returns the tile value at the specified x and y coordinate.
     */
    public Terrain getTile (int xx, int yy)
    {
        int index = yy * _width + xx;
        if (index < 0 || index >= _tiles.length) {
            log.warning("Requested to get OOB tile " +
                        "[x=" + xx + ", y=" + yy + "].");
            Thread.dumpStack();
            return Terrain.NONE;
        } else {
            return Terrain.fromCode(_tiles[index]);
        }
    }

    /**
     * Updates the tile value at the specified x and y coordinate.
     */
    public boolean setTile (int xx, int yy, Terrain tile)
    {
        int index = yy * _width + xx;
        if (index < 0 || index >= _tiles.length) {
            log.warning("Requested to set OOB tile [x=" + xx + ", y=" + yy +
                        ", tile=" + tile + "].");
            Thread.dumpStack();
        } else if (_tiles[index] != tile.code) {
            _tiles[index] = tile.code;
            return true;
        }
        return false;
    }

    /**
     * Computes and returns a path for the specified piece to the
     * specified coordinates. Returns null if no path could be found.
     */
    public List computePath (Piece piece, int tx, int ty)
    {
//         log.info("Computing path from " + piece.x + "/" + piece.y + " to " +
//                  tx + "/" + ty + " maxdist:" + piece.getMoveDistance() + ".");
        return AStarPathUtil.getPath(
            _tpred, piece.getStepper(), piece, piece.getMoveDistance(),
            piece.x, piece.y, tx, ty, true);
    }

    /**
     * Returns a set of coordinates for locations near to the specified
     * coordinates into which a piece can be spawned. First the
     * coordinates immediately surrounding the location are searched, then
     * one unit away, and so on. Within a particular "shell" the
     * coordinates are searched randomly. The list may be fewer than the
     * requested count if an insufficient number of spots could be located
     * within the specified maximum distance.
     */
    public ArrayList<Point> getOccupiableSpots (
        int count, int cx, int cy, int maxdist)
    {
        ArrayList<Point> ospots = new ArrayList<Point>();
        PointSet spots = new PointSet();
      SEARCH:
        for (int dist = 0; dist < 3; dist++) {
            spots.clear();
            spots.addFrame(cx, cy, dist, getBounds());
            int[] coords = spots.toIntArray();
            ArrayUtil.shuffle(coords);
            for (int ii = 0; ii < coords.length; ii++) {
                int hx = PointSet.decodeX(coords[ii]);
                int hy = PointSet.decodeY(coords[ii]);
                if (isOccupiable(hx, hy)) {
                    ospots.add(new Point(hx, hy));
                    if (ospots.size() == count) {
                        break SEARCH;
                    }
                }
            }
        }
        return ospots;
    }

    /**
     * Returns the coordinates of a location near to the specified
     * coordinates into which a piece can be spawned. First the
     * coordinates immediately surrounding the location are searched, then
     * one unit away, and so on. Within a particular "shell" the
     * coordinates are searched randomly. Returns null if no occupiable
     * spot could be located.
     */
    public Point getOccupiableSpot (int cx, int cy, int maxdist)
    {
        ArrayList<Point> spots = getOccupiableSpots(1, cx, cy, maxdist);
        return (spots.size() > 0) ? spots.get(0) : null;
    }

    /**
     * Adds the supplied set of pieces to our board "shadow" data. This is
     * done at the start of the game; all subsequent changes are
     * incremental.
     */
    public void shadowPieces (Iterator iter)
    {
        // start out with _tstate configured according to the board
        for (int yy = 0; yy < _height; yy++) {
            for (int xx = 0; xx < _width; xx++) {
                byte tvalue;
                switch (getTile(xx, yy)) {
                case NONE: tvalue = (byte)3; break;
                case WATER: tvalue = (byte)2; break;
                default: tvalue = (byte)0; break;
                }
                int pos = _width*yy+xx;
                _tstate[pos] = tvalue;
                _btstate[pos] = tvalue;
            }
        }

        while (iter.hasNext()) {
            updateShadow(null, (Piece)iter.next());
        }
    }

    /**
     * Updates the shadow for the specified piece.
     */
    public void updateShadow (Piece opiece, Piece piece)
    {
        // unshadow the piece's old position (big pieces never move)
        if (opiece != null) {
            int pos = _width*opiece.y+opiece.x;
            _tstate[pos] = _btstate[pos];
        }

        // now add a shadow for the new piece
        if (piece != null) {
            if (piece instanceof BigPiece) {
                Rectangle pbounds = ((BigPiece)piece).getBounds();
                for (int yy = pbounds.y, ly = yy + pbounds.height;
                     yy < ly; yy++) {
                    for (int xx = pbounds.x, lx = xx + pbounds.width;
                         xx < lx; xx++) {
                        if (_bbounds.contains(xx, yy)) {
                            _tstate[_width*yy+xx] = 2;
                            _btstate[_width*yy+xx] = 2;
                            _estate[_width*yy+xx] = 2;
                        }
                    }
                }

            } else if (piece instanceof Bonus) {
                _tstate[_width*piece.y+piece.x] = 1;

            } else {
                _tstate[_width*piece.y+piece.x] = 3;
            }
        }
    }

    /**
     * Returns the elevation at the specified coordinates (currently only
     * non-zero when there's a building at that tile location).
     */
    public int getElevation (int x, int y)
    {
        return _estate[y*_width+x];
    }

    /**
     * Returns true if the specified location is ground traversable.
     */
    public boolean isGroundOccupiable (int x, int y)
    {
        if (!_bbounds.contains(x, y)) {
            return false;
        }
        return (_btstate[y*_width+x] < 2);
    }

    /**
     * Returns true if the specified piece can occupy the specified
     * coordinate.
     */
    public boolean canOccupy (Piece piece, int x, int y)
    {
        if (!_bbounds.contains(x, y)) {
            return false;
        }
        int max = 1;
        if (piece.isFlyer()) {
            max = 2;
        }
        return (_tstate[y*_width+x] <= max);
    }

    /**
     * Returns true if the specified coordinate is both unoccupied by any
     * other piece and traversable.
     */
    public boolean isOccupiable (int x, int y)
    {
        if (!_bbounds.contains(x, y)) {
            return false;
        }
        return (_tstate[y*_width+x] <= 0);
    }

    /**
     * Computes the supplied piece's move sets based on its current
     * location and the state of the board.
     */
    public void computeMoves (Piece piece, PointSet moves, PointSet attacks)
    {
        // clear out the planning grid
        Arrays.fill(_pgrid, (byte)0);

        int mdist = piece.getMoveDistance();
//         log.info("Recomputing sets for " + piece.info() +
//                  " [mdist=" + mdist + "].");

        // start with 10x our movement points at our current coordinate
        // (and add one to ensure that we always end up with 1 in our
        // final coordinate)
        byte remain = (byte)(mdist * BASE_TRAVERSAL + 1);
        _pgrid[piece.y*_width+piece.x] = remain;

        // now consider each of our four neighbors
        considerMoving(piece, moves, piece.x+1, piece.y, remain);
        considerMoving(piece, moves, piece.x-1, piece.y, remain);
        considerMoving(piece, moves, piece.x, piece.y+1, remain);
        considerMoving(piece, moves, piece.x, piece.y-1, remain);

        // if the attack set is non-null, compute our attacks as well
        if (attacks != null) {
            PointSet set = moves;
            remain = (byte)piece.getFireDistance();
            considerFiring(attacks, piece.x, piece.y, remain, false);
            for (int ii = 0, ll = moves.size(); ii < ll; ii++) {
                int mx = moves.getX(ii), my = moves.getY(ii);
                considerFiring(attacks, mx, my, remain, false);
            }
        }
    }

    /**
     * Computes a set of possible attacks given the specified fire
     * distance.
     */
    public void computeAttacks (
        int fireDistance, int px, int py, PointSet attacks)
    {
        for (int dd = 1; dd <= fireDistance; dd++) {
            for (int xx = px, yy = py - dd; yy < py; xx++, yy++) {
                if (_bbounds.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px + dd, yy = py; xx > px; xx--, yy++) {
                if (_bbounds.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px, yy = py + dd; yy > py; xx--, yy--) {
                if (_bbounds.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px - dd, yy = py; xx < px; xx++, yy--) {
                if (_bbounds.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
        }
    }

    /** Returns a string representation of this board. */
    public String toString ()
    {
        return "[" + _width + "x" + _height + "]";
    }

    /**
     * Extends default behavior to initialize transient members.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        int size = _width*_height;
        _btstate = new byte[size];
        _estate = new byte[size];
        _tstate = new byte[size];
        _pgrid = new byte[size];
        _bbounds = new Rectangle(0, 0, _width, _height);
    }

    /** Helper function for {@link #computeMoves}. */
    protected void considerMoving (
        Piece piece, PointSet moves, int xx, int yy, byte remain)
    {
        // make sure this coordinate is occupiable
        if (!_bbounds.contains(xx, yy) || !canOccupy(piece, xx, yy)) {
            return;
        }

        // see if we can move into this square with a higher remaining
        // point count than has already been accomplished
        int pos = yy*_width+xx;
        byte premain = (byte)(remain - piece.traversalCost(getTile(xx, yy)));
        byte current = _pgrid[pos];
        if (premain <= current) {
            return;
        }

        // if so, do it
        moves.add(xx, yy);
        _pgrid[pos] = premain;

        // and then check all of our neighbors
        considerMoving(piece, moves, xx+1, yy, premain);
        considerMoving(piece, moves, xx-1, yy, premain);
        considerMoving(piece, moves, xx, yy+1, premain);
        considerMoving(piece, moves, xx, yy-1, premain);
    }

    /** Helper function for {@link #computeMoves}. */
    protected void considerFiring (PointSet attacks, int xx, int yy, byte remain,
                                   boolean checkThisSpot)
    {
        // make sure this coordinate is on the board
        if (!_bbounds.contains(xx, yy)) {
            return;
        }

        byte premain = remain;
        if (checkThisSpot) {
            // if this position is non-zero bail
            int pos = yy*_width+xx;
            if (_pgrid[pos] != 0) {
                return;
            }

            // otherwise fill it with our current remaining fire distance
            attacks.add(xx, yy);
            premain = (byte)(remain - 1);
            _pgrid[pos] = (byte)(-1 * premain);
        }

        // and then check all of our neighbors
        if (premain > 0) {
            considerFiring(attacks, xx+1, yy, premain, true);
            considerFiring(attacks, xx-1, yy, premain, true);
            considerFiring(attacks, xx, yy+1, premain, true);
            considerFiring(attacks, xx, yy-1, premain, true);
        }
    }

    /** Used when path finding. */
    protected transient AStarPathUtil.TraversalPred _tpred =
        new AStarPathUtil.TraversalPred() {
        public boolean canTraverse (Object traverser, int x, int y) {
            return canOccupy((Piece)traverser, x, y);
        }
    };

    /** The width and height of our board. */
    protected int _width, _height;

    /** Contains a 2D array of tiles, defining the terrain. */
    protected int[] _tiles;

    /** Tracks coordinate traversability. */
    protected transient byte[] _tstate, _btstate, _estate;

    /** A temporary array for computing move and fire sets. */
    protected transient byte[] _pgrid;

    /** A rectangle containing our bounds, used when path finding. */
    protected transient Rectangle _bbounds;
}
