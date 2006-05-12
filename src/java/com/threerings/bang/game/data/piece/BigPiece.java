//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Rectangle;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;

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

    /** Returns the elevation of this piece in tiles. */
    public float getElevation ()
    {
        return 2f;
    }
    
    /** Checks whether this piece is "tall," meaning that even air units cannot
     * pass over it. */
    public boolean isTall ()
    {
        return false;
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
    public int getHeight ()
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
    protected BigPiece (int width, int height)
    {
        _width = width;
        _height = height;
        recomputeBounds();
    }

    @Override // documentation inherited
    protected void recomputeBounds ()
    {
        if (orientation == NORTH || orientation == SOUTH) {
            _bounds.setBounds(x, y, _width, _height);
        } else {
            _bounds.setBounds(x, y, _height, _width);
        }
    }

    protected int _width, _height;
    protected transient Rectangle _bounds = new Rectangle();
}
