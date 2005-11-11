//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;

/**
 * A train car or engine.
 */
public class Train extends Piece
{
    /** The id of the piece to which this is attached, or 0 for the engine. */
    public int attachId;
    
    /** The last occupied position of the piece. */
    public transient int lastX, lastY;
    
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
        return new MobileSprite("extras", attachId == 0 ? "bison" : "cow");
    }
    
    @Override // documentation inherited
    protected void updatePosition (int nx, int ny)
    {
        lastX = x;
        lastY = y;
        super.updatePosition(nx, ny);
    }
}
