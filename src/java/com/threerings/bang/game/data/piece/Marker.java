//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.MarkerSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;

/**
 * A marker piece class used for a variety of purposes.
 */
public class Marker extends Piece
{
    /** A particular marker type. */
    public static final int START = 0;

    /** A particular marker type. */
    public static final int BONUS = 1;

    /** A particular marker type. */
    public static final int CATTLE = 2;

    /** A particular marker type. */
    public static final int LODE = 3;

    /** A particular marker type. */
    public static final int TOTEM = 4;

    /** A particular marker type. */
    public static final int SAFE = 5;

    /** A particular marker type. */
    public static final int ROBOTS = 6;

    /** A particular marker type. */
    public static final int TALISMAN = 7;

    /** A particular marker type. */
    public static final int FETISH = 8;

    /** A particular marker type. */
    public static final int SAFE_ALT = 9;

    /**
     * Handy function for checking if this piece is a marker and of the
     * specified type.
     */
    public static boolean isMarker (Piece piece, int type)
    {
        return (piece instanceof Marker) && ((Marker)piece).getType() == type;
    }

    /**
     * Creates a marker of the specified type.
     */
    public Marker (int type)
    {
        _type = type;
    }

    /**
     * Creates a blank marker for use when unserializing.
     */
    public Marker ()
    {
    }

    /**
     * Returns the type of this marker.
     */
    public int getType ()
    {
        return _type;
    }

    public void setType (int type)
    {
        _type = type;
    }

    @Override // documentation inherited
    public int computeElevation (
            BangBoard board, int tx, int ty, boolean moving)
    {
        if (_type == START) {
            return super.computeElevation(board, tx, ty, moving);
        }
        return board.getElevation(tx, ty);
    }

    @Override // documentation inherited
    public int getGoalRadius (Piece mover)
    {
        return (_type == SAFE) ? 0 : -1;
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new MarkerSprite(_type);
    }

    /**
     * Returns true if we want to use the marker sprite in game.
     */
    public boolean addSprite ()
    {
        return _type == SAFE || _type == SAFE_ALT;
    }

    @Override // documentation inherited
    protected int computeOrientation (int nx, int ny)
    {
        // our orientation doesn't change with position
        return orientation;
    }

    /** Indicates the type of this marker. */
    protected int _type;
}
