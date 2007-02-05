//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.game.client.sprite.TargetableActiveSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ClearPieceEffect;

import static com.threerings.bang.Log.log;

/**
 * Handles breakable pieces in Boom Town.
 */
public class Breakable extends Prop
{
    @Override // documentation inherited
    public boolean isTargetable ()
    {
        return true;
    }    
    
    @Override // documentation inherited
    public ArrayList<Effect> tick (
            short tick, BangObject bangobj, Piece[] pieces)
    {
        if (lastActed != tick-1 || isAlive()) {
            return null;
        }

        ArrayList<Effect> effects = new ArrayList<Effect>();
        effects.add(new ClearPieceEffect(this));
        return effects;
    }

    @Override // documentation inherited
    public int computeElevation (BangBoard board, int tx, int ty, boolean moving)
    {
        return board.getElevation(tx, ty);
    }
 
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TargetableActiveSprite("props", "boom_town/breakables/breakable") {
            public boolean removed () {
                queueAction(REMOVED);
                return true;
            }
            
            public boolean isHoverable ()
            {
                return true;
            }
            
            @Override // documentation inherited
            public void updated (Piece piece, short tick)
            {
                super.updated(piece, tick);

                _target.updated(piece, tick);
            }            
        };
    }
}
