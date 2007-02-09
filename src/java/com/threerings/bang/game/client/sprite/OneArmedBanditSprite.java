//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.CounterInterface;
import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.sprite.UnitSprite;


/**
 * Sprite for one-armed bandit.
 */
 public class OneArmedBanditSprite extends UnitSprite
 {
     public GenericCounterNode counter;
     
     public OneArmedBanditSprite (String type)
     {
         super(type);
     }

     public void init (
         BasicContext ctx, BoardView view, BangBoard board, 
         com.threerings.openal.SoundGroup sounds, Piece piece, short tick)
     {
         super.init(ctx, view, board, sounds, piece, tick);
         counter = new GenericCounterNode();
         counter.createGeometry((CounterInterface)piece, (BangContext)ctx);
         this.attachChild(counter);
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
 }