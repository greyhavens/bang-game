//
// $Id$

package com.samskivert.bang.data;

import com.threerings.io.SimpleStreamableObject;

import com.samskivert.bang.data.piece.Piece;

/**
 * Contains a set of waypoints that will be traveled toward by a
 * particular piece.
 */
public class PiecePath extends SimpleStreamableObject
{
    /** The id of the piece that will move along this path. */
    public int pieceId;

    /** Creates a piece path with the specified starting goal. */
    public PiecePath (int pieceId, int x, int y)
    {
        this.pieceId = pieceId;
        _coords = new int[] { x, y };
    }

    /** Constructor used for unserialization. */
    public PiecePath ()
    {
    }

    /** Appends the supplied coordinates to this path. */
    public PiecePath append (int x, int y)
    {
        PiecePath path = new PiecePath();
        path.pieceId = pieceId;
        path._coords = new int[_coords.length+2];
        System.arraycopy(_coords, 0, path._coords, 0, _coords.length);
        path._coords[_coords.length] = x;
        path._coords[_coords.length+1] = y;
        return path;
    }

    /** Returns the length of this path. */
    public int getLength ()
    {
        return _coords.length / 2;
    }

    /** Returns the x coordinate of the specified position. */
    public int getX (int position)
    {
        return _coords[position*2];
    }

    /** Returns the y coordinate of the specified position. */
    public int getY (int position)
    {
        return _coords[position*2+1];
    }

    /** Returns the x coordinate of our next goal. */
    public int getNextX (Piece piece)
    {
        return getX(Math.min(piece.pathPos, getLength()-1));
    }

    /** Returns the y coordinate of our next goal. */
    public int getNextY (Piece piece)
    {
        return getY(Math.min(piece.pathPos, getLength()-1));
    }

    /**
     * Returns true if the specified node is the tail of this path.
     */
    public boolean isTail (int x, int y)
    {
        return (x == _coords[_coords.length-2] &&
                y == _coords[_coords.length-1]);
    }

    /**
     * Called to indicate that our piece reached the current goal.
     *
     * @return false if there are yet more goals to reach, true if the
     * path is completed.
     */
    public boolean reachedGoal (Piece piece)
    {
        return (++piece.pathPos >= getLength());
    }

    // documentation inherited
    public String toString ()
    {
        return "[pid=" + pieceId + ", l=" + getLength() + "]";
    }

    /** Contains the coordinates of our various goals. */
    protected int[] _coords;
}
