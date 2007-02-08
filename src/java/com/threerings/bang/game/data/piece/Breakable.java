//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TargetableActiveSprite;
import com.threerings.bang.game.client.sprite.GenericCounterNode;
import com.threerings.bang.game.client.effect.ExplosionViz;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ExplodeEffect;
import com.threerings.bang.game.data.piece.CounterInterface;

import static com.threerings.bang.Log.log;

/**
 * Handles breakable pieces in Boom Town.
 */
public class Breakable extends Prop
    implements CounterInterface
{
    public class BreakableSprite extends TargetableActiveSprite
    {
        public GenericCounterNode counter;
        
        public BreakableSprite (String type, String name) 
        {
            super(type, name);
        }

        public void init (
            BasicContext ctx, BoardView view, BangBoard board, 
            com.threerings.openal.SoundGroup sounds, Piece piece, short tick)
        {
            super.init(ctx, view, board, sounds, piece, tick);
            counter = new GenericCounterNode();
            this.attachChild(counter);
            counter.createGeometry((CounterInterface)piece, (BangContext)ctx);
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
            counter.updateCount((CounterInterface)piece);
        }            
    };
        
    public void init()
    {
        damage = 0;
    }
    
    public int getCount()
    {
        return _count;
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
        return (PieceSprite)new BreakableSprite("props", "boom_town/breakables/breakable");
    }

    protected boolean isExploding = false;
    protected int _count = 10;
}
