//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.HashMap;

import com.threerings.bang.data.Stat;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TotemSprite;
import com.threerings.bang.game.data.effect.BonusEffect;
import com.threerings.bang.game.data.effect.TotemEffect;

/**
 * Special code to handle totem bonuses.
 */
public class TotemBonus extends Bonus
{
    public static enum Type
    {
        TOTEM_LARGE(TotemEffect.TOTEM_LARGE_BONUS, 30, 3, 
                Stat.Type.TOTEMS_LARGE),
        TOTEM_MEDIUM(TotemEffect.TOTEM_MEDIUM_BONUS, 20, 2,
                Stat.Type.TOTEMS_MEDIUM),
        TOTEM_SMALL(TotemEffect.TOTEM_SMALL_BONUS, 10, 1,
                Stat.Type.TOTEMS_SMALL),
        TOTEM_CROWN(TotemEffect.TOTEM_CROWN_BONUS, 40, 4,
                Stat.Type.TOTEMS_CROWN);

        Type (String bonus, int value, int height, Stat.Type stat) {
            _bonus = bonus;
            _value = value;
            _height = height;
            _stat = stat;
        }

        public String bonus () {
            return _bonus;
        }

        public int value () {
            return _value;
        }

        public int height () {
            return _height;
        }

        public Stat.Type stat () {
            return _stat;
        }

        protected String _bonus;
        protected int _height;
        protected int _value;
        protected Stat.Type _stat;
    }

    public static final HashMap<String, Type> TOTEM_LOOKUP =
        new HashMap<String, Type>();

    static {
        TOTEM_LOOKUP.put(TotemEffect.TOTEM_LARGE_BONUS, Type.TOTEM_LARGE);
        TOTEM_LOOKUP.put(TotemEffect.TOTEM_MEDIUM_BONUS, Type.TOTEM_MEDIUM);
        TOTEM_LOOKUP.put(TotemEffect.TOTEM_SMALL_BONUS, Type.TOTEM_SMALL);
        TOTEM_LOOKUP.put(TotemEffect.TOTEM_CROWN_BONUS, Type.TOTEM_CROWN);
    }

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
