//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.client.effect.DamageIconViz;
import com.threerings.bang.game.data.effect.TrainEffect;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Handles displaying the proper damage value with a damage icon during 
 * a collision.
 */
public class CollisionHandler extends EffectHandler
{
    public CollisionHandler (int damage)
    {
        _damage = damage;
    }

    public void pieceAffected (Piece piece, String effect)
    {
        super.pieceAffected(piece, effect);
        if (effect.equals(TrainEffect.DAMAGED)) {
            DamageIconViz.displayDamageIconViz(piece, _damage, _ctx, _view);
        }
    }

    protected int _damage;
}
