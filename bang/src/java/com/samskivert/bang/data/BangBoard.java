//
// $Id$

package com.samskivert.bang.data;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.media.util.AStarPathUtil;

import com.samskivert.bang.data.PiecePath;
import com.samskivert.bang.data.piece.BigPiece;
import com.samskivert.bang.data.piece.Bonus;
import com.samskivert.bang.data.piece.Chopper;
import com.samskivert.bang.data.piece.Piece;

import static com.samskivert.bang.Log.log;

/**
 * Describes the terrain of the game board.
 */
public class BangBoard extends SimpleStreamableObject
{
    /** Creates a board with the specified dimensions. */
    public BangBoard (int width, int height)
    {
        _width = width;
        _height = height;
        _tiles = new int[width*height];
        _tstate = new byte[width*height];
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
        if (index >= _tiles.length) {
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
    public void setTile (int xx, int yy, Terrain tile)
    {
        int index = yy * _width + xx;
        if (index >= _tiles.length) {
            log.warning("Requested to set OOB tile [x=" + xx + ", y=" + yy +
                        ", tile=" + tile + "].");
            Thread.dumpStack();
        } else {
            _tiles[index] = tile.code;
        }
    }

    /**
     * Computes and returns a path for the specified piece to the
     * specified coordinates. Returns null if no path could be found.
     */
    public PiecePath computePath (Piece piece, int tx, int ty)
    {
        List path = AStarPathUtil.getPath(
            _tpred, piece.getStepper(), piece, _width+_height, piece.x, piece.y,
            tx, ty, true);
        if (path.size() > 1) {
            // the first coordinate is the piece's current coordinate
            path.remove(0);
            return new PiecePath(piece.pieceId, path);
        }
        return null;
    }

    /**
     * Updates the "shadow" we use to compute paths around the board.
     */
    public void updatePathData (DSet pieces)
    {
        Arrays.fill(_tstate, (byte)0);
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            if (piece instanceof BigPiece) {
                Rectangle pbounds = ((BigPiece)piece).getBounds();
                for (int yy = pbounds.y, ly = yy + pbounds.height;
                     yy < ly; yy++) {
                    for (int xx = pbounds.x, lx = xx + pbounds.width;
                         xx < lx; xx++) {
                        _tstate[_width*yy+xx] = 1;
                    }
                }
            } else if (!(piece instanceof Bonus)) {
                _tstate[_width*piece.y+piece.x] = 2;
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
        _tstate = new byte[_width*_height];
        _bbounds = new Rectangle(0, 0, _width, _height);
    }

    /** Used when path finding. */
    protected transient AStarPathUtil.TraversalPred _tpred =
        new AStarPathUtil.TraversalPred() {
        public boolean canTraverse (Object traverser, int x, int y) {
            if (!_bbounds.contains(x, y)) {
                return false;
            }
            int max = 0;
            if (traverser instanceof Chopper) {
                max = 1;
            }
            return (_tstate[y*_bbounds.width+x] <= max);
        }
    };

    /** The width and height of our board. */
    protected int _width, _height;

    /** Contains a 2D array of tiles, defining the terrain. */
    protected int[] _tiles;

    /** Tracks coordinate traversability. */
    protected transient byte[] _tstate;

    /** A rectangle containing our bounds, used when path finding. */
    protected transient Rectangle _bbounds;
}
