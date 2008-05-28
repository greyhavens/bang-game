//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;
import java.util.HashMap;

import com.threerings.bang.data.StatType;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TotemSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.BonusEffect;
import com.threerings.bang.game.data.effect.TotemEffect;
import com.threerings.bang.game.util.PointSet;

/**
 * Special code to handle totem bonuses.
 */
public class TotemBonus extends Bonus
{
    public static enum Type
    {
        TOTEM_LARGE(TotemEffect.TOTEM_LARGE_BONUS, 30, 3, 0,
                StatType.TOTEMS_LARGE),
        TOTEM_MEDIUM(TotemEffect.TOTEM_MEDIUM_BONUS, 20, 2, 20,
                StatType.TOTEMS_MEDIUM),
        TOTEM_SMALL(TotemEffect.TOTEM_SMALL_BONUS, 10, 1, 40,
                StatType.TOTEMS_SMALL),
        TOTEM_CROWN(TotemEffect.TOTEM_CROWN_BONUS, 40, 4, 0,
                StatType.TOTEMS_CROWN);

        Type (String bonus, int value, int height, int damage, StatType stat) {
            _bonus = bonus;
            _value = value;
            _height = height;
            _damage = damage;
            _stat = stat.ordinal();
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

        public int damage () {
            return _damage;
        }

        public StatType stat () {
            return StatType.values()[_stat];
        }

        protected String _bonus;
        protected int _height, _value, _damage;
        protected int _stat;
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
    public BonusEffect affect (BangObject bangobj, Piece piece)
    {
        TotemEffect effect = (TotemEffect)super.affect(bangobj, piece);
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

    @Override // documentation inherited
    public Point getDropLocation (BangObject bangobj)
    {
        // try not to drop a totem bonus next to a totem base
        PointSet reserved = new PointSet();
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof TotemBase) {
                for (int ii = 0; ii < DIRECTIONS.length; ii++) {
                    reserved.add(piece.x + DX[ii], piece.y + DY[ii]);
                }
            }
        }
        return bangobj.board.getOccupiableSpot(x, y, 1, 10, null, reserved);
    }
}
