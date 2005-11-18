//
// $Id$

package com.threerings.bang.game.client;

import java.util.Iterator;

import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;

import com.threerings.openal.SoundGroup;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.StampedeEffect;

import com.threerings.bang.util.BangContext;

/**
 * Displays a stampede.
 */
public class StampedeHandler extends EffectHandler
    implements PathObserver
{
    @Override // documentation inherited
    public void init (BangContext ctx, BangObject bangobj, BangBoardView view,
                      SoundGroup sounds, Effect effect)
    {
        super.init(ctx, bangobj, view, sounds, effect);
        _stampede = (StampedeEffect)effect;
        
        // wait for all pieces to stop moving
        for (Iterator it = bangobj.pieces.iterator(); it.hasNext(); ) {
            Piece piece = (Piece)it.next();
            PieceSprite sprite = view.getPieceSprite(piece);
            if (sprite.isMoving()) {
                sprite.addObserver(this);
                _sprites++;
            }
        }
        if (_sprites == 0) {
            startStampede();
        }                
    }
    
    // documentation inherited from interface PathObserver
    public void pathCompleted (Sprite sprite, Path path)
    {
        sprite.removeObserver(this);
        if (--_sprites == 0) {
            startStampede();
        }
    }

    // documentation inherited from interface PathObserver
    public void pathCancelled (Sprite sprite, Path path)
    {
        sprite.removeObserver(this);
        if (--_sprites == 0) {
            startStampede();
        }
    }
    
    /**
     * Sets the stampede in motion.
     */
    protected void startStampede ()
    {
        for (int i = 0; i < _stampede.paths.length; i++) {
            MobileSprite sprite = new MobileSprite("extras", "bison");
            sprite.init(_ctx, _view, _sounds, new DummyPiece(), _bangobj.tick);
            _view.addSprite(sprite);
            sprite.move(_bangobj.board, _stampede.paths[i],
                StampedeEffect.BUFFALO_SPEED);
            sprite.addObserver(_remover);
        }
    }
    
    /** A dummy piece for the buffalo sprites. */
    protected class DummyPiece extends Piece
    {
    }
    
    protected PathObserver _remover = new PathObserver() {
        public void pathCompleted (Sprite sprite, Path path) {
            _view.removeSprite(sprite);
        }
        public void pathCancelled (Sprite sprite, Path path) {
            _view.removeSprite(sprite);
        }
    };
    
    protected StampedeEffect _stampede;
    protected int _sprites;
}
