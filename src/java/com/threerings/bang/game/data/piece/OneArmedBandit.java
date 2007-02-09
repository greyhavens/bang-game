//
// $Id$

package com.threerings.bang.game.data.piece;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.CounterInterface;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.RandomInfluenceEffect;
import com.threerings.bang.game.data.effect.UpdateEffect;

import com.threerings.bang.game.client.sprite.GenericCounterNode;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.OneArmedBanditSprite;


/**
 * Handles some special custom behavior needed for the One Armed Jack.
 */
public class OneArmedBandit extends Unit
    implements CounterInterface
{
    // from CounterInterface
    public int getCount()
    {
        return _kind.ordinal();
    }    
    
    @Override // documentation inherited
    public Effect[] maybeGeneratePostOrderEffects ()
    {
        _kind = RandomUtil.pickRandom(RandomInfluenceEffect.Kind.values());
        Effect randomEffect = (_kind != RandomInfluenceEffect.Kind.EXPLODE) ?
            new RandomInfluenceEffect(this.pieceId, _kind) : new AreaDamageEffect(owner, 100, 1, x, y);
        Effect[] effects = super.maybeGeneratePostOrderEffects();
        effects = (effects == NO_EFFECTS) ? new Effect[] { randomEffect } : ArrayUtil.append(effects, randomEffect);
        effects = ArrayUtil.append(effects, new UpdateEffect(this));
        return effects;
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new OneArmedBanditSprite(_config.type);
    }
    
    RandomInfluenceEffect.Kind _kind = RandomUtil.pickRandom(RandomInfluenceEffect.Kind.values());
}
