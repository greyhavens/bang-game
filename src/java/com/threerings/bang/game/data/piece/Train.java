//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TrainSprite;
import com.threerings.bang.game.data.BangBoard;

/**
 * A train car or engine.
 */
public class Train extends Piece
{
    /** The engine that leads the train. */
    public static final byte ENGINE = 0;
    
    /** The caboose that tails the train. */
    public static final byte CABOOSE = 1;
    
    /** A car carrying cattle. */
    public static final byte CATTLE_CAR = 2;
    
    /** A car carrying freight. */
    public static final byte FREIGHT_CAR = 3;
    
    /** The types of cars to insert between the engine and the caboose. */
    public static final byte[] CAR_TYPES = { CATTLE_CAR, FREIGHT_CAR };
    
    /** A special value indicating that a short value is unset. */
    public static final short UNSET = Short.MIN_VALUE;
    
    /** The type of train piece: engine, caboose, etc. */
    public byte type;
    
    /** The last occupied position of the piece. */
    public short lastX = UNSET, lastY = UNSET;
    
    /** The next position that the piece will occupy. */
    public short nextX, nextY;
    
    /**
     * Determines whether the specified piece is behind this one.
     */
    public boolean isBehind (Piece piece)
    {
        return piece.x == (x + REV_X_MAP[orientation]) &&
            piece.y == (y + REV_Y_MAP[orientation]);
    }
    
    @Override // documentation inherited
    public boolean removeWhenDead ()
    {
        return true;
    }
    
    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return !lapper.isFlyer() && !(lapper instanceof Track);
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TrainSprite(type);
    }
    
    /**
     * Updates the next position to be occupied by this train, recording the
     * current position as the last position, moving the train into the
     * previously stored next position, and storing the new next position.
     */
    public void positionNext (int nx, int ny)
    {
        lastX = x;
        lastY = y;
        
        position(nextX, nextY);
        
        nextX = (short)nx;
        nextY = (short)ny;
    }
}
