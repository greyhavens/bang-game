//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.threerings.openal.SoundGroup;

import java.util.ArrayList;

import com.jme.scene.Spatial;
import com.jme.math.FastMath;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.ParticleUtil;

import com.threerings.bang.game.client.effect.WreckViz;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.CounterInterface;
import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.sprite.TargetableActiveSprite;
import com.threerings.bang.client.util.ResultAttacher;



/**
 * Sprite for Breakables
 */
 public class BreakableSprite extends TargetableActiveSprite
 {
     public GenericCounterNode counter;

     public BreakableSprite (String type, String name)
     {
         super(type, name);
     }

     public void init (
         BasicContext ctx, BoardView view, BangBoard board,
         SoundGroup sounds, Piece piece, short tick)
     {
         super.init(ctx, view, board, sounds, piece, tick);
         counter = new GenericCounterNode();
         counter.createGeometry((CounterInterface)piece, ctx);
         attachChild(counter);
     }

     @Override // documentation inherited
     public void updated (Piece piece, short tick)
     {
         super.updated(piece, tick);
         _target.updated(piece, tick);

        if (_piece.isAlive()) {
            counter.updateCount((CounterInterface)piece);
            if (_smoke == null) {
                _ctx.loadParticles("frontier_town/fire", new ResultAttacher<Spatial>(this) {
                    public void requestCompleted (Spatial result) {
                        super.requestCompleted(result);
                        _smoke = result;
                    }
                });
            }
        } else {
            // remove all effects
            if (_smoke != null) {
                ParticleUtil.stopAndRemove(_smoke);
                _smoke = null;
                ArrayList<Spatial> children = getChildren();
                for (Spatial child : children){
                    if (!(child instanceof WreckViz.Wreckage))
                    {
                        child.setCullMode(Spatial.CULL_ALWAYS);
                    }
                }
                if (_status != null) {
                    _status.detachAllChildren();
                }
            }
        }
     }

     public boolean removed ()
     {
         queueAction(REMOVED);
         return true;
     };

     protected Spatial _smoke;
 }