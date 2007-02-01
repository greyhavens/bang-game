//
// $Id$

package com.threerings.bang.game.data.piece;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.RandomInfluenceEffect;

/**
 * Handles some special custom behavior needed for the One Armed Jack.
 */
public class OneArmedBandit extends Unit
{
    @Override // documentation inherited
    public Effect[] maybeGeneratePostOrderEffects ()
    {
        RandomInfluenceEffect.Kind kind = RandomUtil.pickRandom(RandomInfluenceEffect.Kind.values());
        Effect randomEffect = (kind != RandomInfluenceEffect.Kind.EXPLODE) ?
            new RandomInfluenceEffect(this.pieceId, kind) : new AreaDamageEffect(owner, 100, 1, x, y);
        
        Effect[] effects = super.maybeGeneratePostOrderEffects();
        return (effects == NO_EFFECTS) ? new Effect[] { randomEffect } : ArrayUtil.append(effects, randomEffect);
    }
}
