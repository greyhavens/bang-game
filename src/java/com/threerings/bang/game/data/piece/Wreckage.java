//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.WreckageSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ClearPieceEffect;

/**
 * A piece of wreckage that will hang out for a while before disappearing.
 */
public class Wreckage extends Piece
{
    /** The number of ticks remaining until this wreckage disappears. */
    public transient int tickCounter = 6;    

    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangBoard board, Piece[] pieces)
    {
        if (tickCounter-- > 0) {
            return null;
        }

        ArrayList<Effect> effects = new ArrayList<Effect>();
        effects.add(new ClearPieceEffect(this));
        return effects;
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new WreckageSprite();
    }
}
