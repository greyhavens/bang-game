//
// $Id$

package com.threerings.bang.game.data.piece;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.RandomInfluenceEffect;
import com.threerings.bang.game.data.effect.UpdateEffect;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.OneArmedBanditSprite;

/**
 * Handles some special custom behavior needed for the One Armed Jack.
 */
public class OneArmedBandit extends Unit
{
    /*implements CounterInterface*/
    public RandomInfluenceEffect.Kind card = RandomInfluenceEffect.Kind.NONE;
    
    /*// from CounterInterface
    public int getCount()
    {
        return card.ordinal();
    }
    */
    public void wasKilled (short tick)
    {
        super.wasKilled(tick);
        card = RandomInfluenceEffect.Kind.NONE;
    }

    @Override // documentation inherited
    public Effect[] maybeGeneratePostOrderEffects ()
    {
        // if the last order was to explode
        Effect randomEffect;
        if (card == RandomInfluenceEffect.Kind.EXPLODE) {
            randomEffect = new AreaDamageEffect(owner, 100, 1, x, y);
        } else {
            card = RandomUtil.pickRandom(RandomInfluenceEffect.Kind.values(), RandomInfluenceEffect.Kind.NONE);
            randomEffect = (card != RandomInfluenceEffect.Kind.EXPLODE) ?
                new RandomInfluenceEffect(this.pieceId, card) : null;
        }

        Effect[] effects = super.maybeGeneratePostOrderEffects();

        if (randomEffect != null) {
            effects = ArrayUtil.append(effects, randomEffect);
        }
        effects = ArrayUtil.append(effects, new UpdateEffect(this));

        return effects;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new OneArmedBanditSprite(_config.type);
    }
}
