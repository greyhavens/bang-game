//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.game.client.sprite.TargetableActiveSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ExplodeEffect;
import com.threerings.bang.game.client.effect.ExplosionViz;

import static com.threerings.bang.Log.log;

/**
 * Handles breakable pieces in Boom Town.
 */
public class Breakable extends Prop
{
    public void init() {
        damage = 99;
    }

    @Override // documentation inherited
    public boolean removeWhenDead ()
    {
        return true;
    }
    
    @Override // documentation inherited
    public Effect willDie (BangObject bangobj, int shooterId)
    {
        if (!isExploding) {
            isExploding = true;
            return new ExplodeEffect(this, 60, 1);
        } else {
            return null;
        }
    }
    
    @Override // documentation inherited
    public boolean isTargetable ()
    {
        return true;
    }    
    
    @Override // documentation inherited
    public ArrayList<Effect> tick (
            short tick, BangObject bangobj, Piece[] pieces)
    {
        return null;
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
    
    protected boolean isExploding = false;
}
