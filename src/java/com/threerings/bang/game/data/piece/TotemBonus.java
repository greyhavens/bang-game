//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TotemSprite;

import com.threerings.bang.game.data.effect.BonusEffect;
import com.threerings.bang.game.data.effect.TotemEffect;

/**
 * Special code to handle totem bonuses.
 */
public class TotemBonus extends Bonus
{
    /**
     * Convenience function to termine if a unit is holding a totem bonus.
     */
    public static boolean isHolding (Unit unit)
    {
        return (unit.holding != null && 
                unit.holding.startsWith("indian_post/totem"));
    }

    @Override // documentation inherited
    public BonusEffect affect (Piece piece)
    {
        TotemEffect effect = (TotemEffect)super.affect(piece);
        if (effect != null) {
            effect.type = _config.type;
        }
        return effect;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TotemSprite(_config.type);
    }
}
