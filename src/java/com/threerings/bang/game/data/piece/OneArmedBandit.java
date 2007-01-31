//
// $Id$

package com.threerings.bang.game.data.piece;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.RamblinEffect;
import com.threerings.bang.game.data.effect.PowerUpEffect;
import com.threerings.bang.game.data.effect.IronPlateEffect;

/**
 * Handles some special custom behavior needed for the One Armed Jack.
 */
public class OneArmedBandit extends Unit
{
    public enum RandomEffectType { UP_MOVE, UP_ATTACK, UP_DEFENSE, EXPLODE };
    
    @Override // documentation inherited
    public Effect[] maybeGeneratePostOrderEffects ()
    {
        Effect[] effects = super.maybeGeneratePostOrderEffects();
        RandomEffectType type = RandomUtil.pickRandom(RandomEffectType.values());
        Effect randomEffect = null;
        switch (type) {
        case UP_MOVE:
            randomEffect = createUpMove();
            break;
        case UP_ATTACK:
            randomEffect = createUpAttack();
            break;
        case UP_DEFENSE:
            randomEffect = createUpDefense();
            break;
        case EXPLODE:
        default:
            randomEffect = createExplode();
            break;
        }
        effects = (effects == NO_EFFECTS) ? new Effect[] { randomEffect } : ArrayUtil.append(effects, randomEffect);
        return effects;
    }

    protected Effect createUpMove() {
        RamblinEffect effect = new RamblinEffect();
        effect.pieceId = pieceId;
        effect.influenceType = Unit.InfluenceType.SPECIAL;
        return (Effect)effect;
    }
    
    protected Effect createUpAttack() {
        PowerUpEffect effect = new PowerUpEffect();
        effect.pieceId = pieceId;
        effect.influenceType = Unit.InfluenceType.SPECIAL;
        return (Effect)effect;
    }

    protected Effect createUpDefense() {
        IronPlateEffect effect = new IronPlateEffect();
        effect.pieceId = pieceId;
        effect.influenceType = Unit.InfluenceType.SPECIAL;
        return (Effect)effect;
    }

    protected Effect createExplode() {
        // die guarateed
        return new AreaDamageEffect(owner, 60, 1, x, y);
    }
}
