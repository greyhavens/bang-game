//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TrackSprite;

/**
 * A piece of track for the train.
 */
public class Track extends Piece
{
    /** An isolated piece of track. */
    public static final byte SINGLETON = 0;
    
    /** A place where the track begins or ends. */
    public static final byte TERMINAL = 1;
    
    /** A straight piece of track. */
    public static final byte STRAIGHT = 2;
    
    /** A T-junction. */
    public static final byte T_JUNCTION = 3;
    
    /** A cross junction. */
    public static final byte X_JUNCTION = 4;
    
    /** A turn. */
    public static final byte TURN = 5;
    
    /** The type of this track (singleton, terminal, etc.) */
    public byte type;
    
    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return false;
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TrackSprite();
    }
    
    @Override // documentation inherited
    protected int computeOrientation (int nx, int ny)
    {
        // our orientation never changes
        return orientation;
    }
}
