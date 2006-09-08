//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Rectangle;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;

import com.threerings.bang.game.data.BangBoard;

/**
 * A base class for pieces that are big rectangles rather than a chain of
 * 1x1 segments. These pieces are not intended for player control, nor to
 * move around the board. Just to sit quietly and be large.
 */
public abstract class BigPiece extends Piece
{
    /** A constructor used when unserializing. */
    public BigPiece ()
    {
    }

    /**
     * Returns the bounds of this big piece. <em>Do not</em> modify the
     * returned rectangle.
     */
    public Rectangle getBounds ()
    {
        return _bounds;
    }

    /** Checks whether this piece is "tall," meaning that even air units cannot
     * pass over it. */
    public boolean isTall ()
    {
        return false;
    }
    
    /** Checks whether this piece is "penetrable," meaning that units can shoot
     * through it.
     */
    public boolean isPenetrable ()
    {
        return false;
    }

    @Override // documentation inherited
    public int computeElevation (BangBoard board, int tx, int ty)
    {
        if (_bounds.width == 1 && _bounds.height == 1) {
            return board.getWaterElevation(tx, ty);
        }
        int elevation = Integer.MIN_VALUE;
        for (int y = ty, ymax = ty + _bounds.height; y < ymax; y++) {
            for (int x = tx, xmax = tx + _bounds.width; x < xmax; x++) {
                elevation = Math.max(elevation,
                    board.getWaterElevation(x, y));
            }
        }
        return elevation;   
    }
    
    @Override // documentation inherited
    public boolean intersects (Rectangle bounds)
    {
        return _bounds.intersects(bounds);
    }

    @Override // documentation inherited
    public boolean intersects (int tx, int ty)
    {
        return _bounds.contains(tx, ty);
    }

    @Override // documentation inherited
    public boolean intersects (Piece other)
    {
        if (other instanceof BigPiece) {
            return _bounds.intersects(((BigPiece)other).getBounds());
        } else {
            return intersects(other.x, other.y);
        }
    }

    @Override // documentation inherited
    public int getWidth ()
    {
        return _bounds.width;
    }

    @Override // documentation inherited
    public int getLength ()
    {
        return _bounds.height;
    }

    /**
     * Extends default behavior to initialize transient members.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        recomputeBounds();
    }

    @Override // documentation inherited
    public Object clone ()
    {
        // make a deep copy of the bounds object
        BigPiece piece = (BigPiece)super.clone();
        piece._bounds = (Rectangle)_bounds.clone();
        return piece;
    }
    
    @Override // documentation inherited
    protected int computeOrientation (int nx, int ny)
    {
        // our orientation never changes
        return orientation;
    }

    /** Require that our derived classes tell us how big they are (in the
     * north/south orientation). */
    protected BigPiece (int width, int length)
    {
        _width = width;
        _length = length;
        recomputeBounds();
    }

    @Override // documentation inherited
    protected void recomputeBounds ()
    {
        if (orientation == NORTH || orientation == SOUTH) {
            _bounds.setBounds(x, y, _width, _length);
        } else {
            _bounds.setBounds(x, y, _length, _width);
        }
    }

    protected int _width, _length;
    protected transient Rectangle _bounds = new Rectangle();
}
