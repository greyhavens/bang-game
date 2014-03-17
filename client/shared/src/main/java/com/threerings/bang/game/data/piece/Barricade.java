//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;
import java.util.List;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ClearPieceEffect;

/**
 * A barricade that will hang out for a while before disappearing.
 */
public class Barricade extends Piece
{
    /** The number of ticks remaining until this barricade disappears. */
    public transient int tickCounter = 6;

    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangObject bangobj, List<Piece> pieces)
    {
        if (tickCounter-- > 0) {
            return null;
        }

        ArrayList<Effect> effects = new ArrayList<Effect>();
        effects.add(new ClearPieceEffect(this));
        return effects;
    }

    @Override // documentation inherited
    public int computeElevation (
            BangBoard board, int tx, int ty, boolean moving)
    {
        return board.getElevation(tx, ty);
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new MobileSprite("extras", "frontier_town/barricade") {
            public boolean removed () {
                queueAction(REMOVED);
                return true;
            }
        };
    }
}
