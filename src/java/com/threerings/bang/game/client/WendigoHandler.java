//
// $Id$

package com.threerings.bang.game.client;

import com.samskivert.util.Interval;

import com.threerings.bang.game.client.effect.DamageIconViz;

import com.threerings.bang.game.client.sprite.WendigoSprite;

import com.threerings.bang.game.data.effect.Effect;

import com.threerings.bang.game.data.effect.StampedeEffect.Collision;

import com.threerings.bang.game.data.effect.WendigoEffect;

import com.threerings.bang.game.data.piece.Piece;

/**
 * Displays a wendigo.
 */
public class WendigoHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        WendigoEffect effect = (WendigoEffect)_effect;

        for (WendigoEffect.Movement m : effect.moves) {
            Piece piece = _bangobj.pieces.get(m.pieceId);
            if (piece != null) {
                _bangobj.board.clearShadow(piece);
                piece.position(m.nx, m.ny);
                WendigoSprite sprite = 
                    (WendigoSprite)_view.getPieceSprite(piece);
                if (sprite != null) {
                    sprite.move(_bangobj.board, m.path, 
                            WendigoEffect.WENDIGO_SPEED, 
                            WendigoHandler.this, notePender());
                }
            }
        }
        
        for (Collision collision : (effect).collisions) {
            new CollisionInterval(collision).schedule();
        }

        return !isCompleted();
    }

    @Override // documentation inherited
    public void pieceAffected (Piece piece, String effect)
    {
        super.pieceAffected(piece, effect);
        if (effect.equals(WendigoEffect.EATEN)) {
            DamageIconViz.displayDamageIconViz(piece, 100, _ctx, _view);
        }
    }

    /**
     * Called to let the handler know the wendigo has finished moving.
     */
    public void pathCompleted (int penderId)
    {
        maybeComplete(penderId);
    }

    /** An interval to activate collisions on their listed timesteps. */
    protected class CollisionInterval extends Interval
    {
        public CollisionInterval (Collision collision)
        {
            super(_ctx.getClient().getRunQueue());
            _collision = collision;
        }

        public void schedule ()
        {
            schedule((long)(1000 * (Math.max(0, _collision.step - 1)) /
                        WendigoEffect.WENDIGO_SPEED));
        }

        public void expired ()
        {
            if (_collision.deathEffect != null) {
                apply(_collision.deathEffect);
            }
            Piece target = (Piece)_bangobj.pieces.get(_collision.targetId);
            if (target != null) {
                Effect.damage(_bangobj, WendigoHandler.this, -1, target, 100,
                        WendigoEffect.EATEN);
            }
        }

        protected Collision _collision;
    }
}
