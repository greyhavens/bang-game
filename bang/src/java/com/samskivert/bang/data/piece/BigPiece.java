//
// $Id$

package com.samskivert.bang.data.piece;

import java.awt.Rectangle;
import java.awt.geom.Point2D;

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
            for (int ii = 0; ii < other.x.length; ii++) {
                if (intersects(other.x[ii], other.y[ii])) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override // documentation inherited
    public Point2D getLocusOfAttention ()
    {
        // a big piece's locus of attention depends on its orientation
        switch (orientation) {
        case NORTH:
            _locus.setLocation(_bounds.x + _bounds.width/2, _bounds.y + 0.5);
            break;
        case EAST:
            _locus.setLocation(_bounds.x + _bounds.width - 0.5,
                               _bounds.y + _bounds.height/2);
            break;
        case SOUTH:
            _locus.setLocation(_bounds.x + _bounds.width/2,
                               _bounds.y + _bounds.height - 0.5);
            break;
        case WEST:
            _locus.setLocation(_bounds.x + 0.5, _bounds.y + _bounds.height/2);
            break;
        }
        return _locus;
    }

    /**
     * Extends default behavior to initialize transient members.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        updateBounds();
    }

    /** Require that our derived classes tell us how big they are (in the
     * north/south orientation). */
    protected BigPiece (int width, int height)
    {
        _width = width;
        _height = height;
    }

    @Override // documentation inherited
    protected void createSegments (int sx, int sy)
    {
        super.createSegments(sx, sy);
        updateBounds();
    }

    @Override // documentation inherited
    protected void pieceMoved ()
    {
        super.pieceMoved();
        updateBounds();
    }

    /** Updates our bounds rectangle based on our position and
     * orientation. */
    protected void updateBounds ()
    {
        if (orientation == NORTH || orientation == SOUTH) {
            _bounds.setBounds(x[0], y[0], _width, _height);
        } else {
            _bounds.setBounds(x[0], y[0], _height, _width);
        }
    }

    protected int _width, _height;
    protected transient Rectangle _bounds = new Rectangle();
}
