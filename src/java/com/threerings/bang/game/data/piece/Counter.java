//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.CounterSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;

/**
 * A prop that maintains a (displayable) count.
 */
public class Counter extends Prop
{
    /** The count. */
    public int count;

    @Override // documentation inherited
    public boolean isOmissible (BangBoard board)
    {
        return false;
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new CounterSprite(_type);
    }
}
