//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TargetableActiveSprite;
import com.threerings.bang.game.client.sprite.GenericCounterNode;
import com.threerings.bang.game.client.effect.ExplosionViz;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.UpdateEffect;
import com.threerings.bang.game.data.effect.DamageEffect;
import com.threerings.bang.game.data.effect.ExplodeEffect;
import com.threerings.bang.game.data.piece.CounterInterface;
import com.threerings.bang.game.client.sprite.BreakableSprite;

import static com.threerings.bang.Log.log;

/**
 * Handles breakable pieces in Boom Town.
 */
public class Breakable extends Prop
    implements CounterInterface
{
    
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
    
    // from CounterInterface
    public int getCount()
    {
        return _count;
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
        if (damage > 1) {
            ArrayList<Effect> effects = new ArrayList<Effect>();
            
            if (_count == -1) {
                // start countdown             
                _count = 3;
            } else {
                _count -= 1;
            }
                
            if (_count == 0) {
                effects.add(new DamageEffect(this, 100));
            } else if (_count > 0) {
                effects.add(new UpdateEffect(this));
            }
            return effects;
        }
            
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
        return new BreakableSprite("props", "boom_town/breakables/breakable");
    }

    protected boolean isExploding = false;
    protected int _count = -1;
}
