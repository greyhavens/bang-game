//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import com.samskivert.util.ArrayUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;

import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;
import java.util.HashMap;

/**
 * Effects that deal with totem pieces.
 */
public class TotemEffect extends HoldEffect
{
    /** The bonus identifier for the totem middle piece. */
    public static final String TOTEM_MIDDLE_BONUS = "indian_post/totem_middle";
    
    /** The bonus identifier for the totem middle piece. */
    public static final String TOTEM_MEDIUM_BONUS = "indian_post/totem_medium";
    
    /** The bonus identifier for the totem middle piece. */
    public static final String TOTEM_SMALL_BONUS = "indian_post/totem_small";

    /** The bonus identifier for the totem crown piece. */
    public static final String TOTEM_CROWN_BONUS = "indian_post/totem_crown";

    public static enum Type
    {
        TOTEM_MIDDLE(TOTEM_MIDDLE_BONUS, 30, 3),
        TOTEM_MEDIUM(TOTEM_MEDIUM_BONUS, 20, 2),
        TOTEM_SMALL(TOTEM_SMALL_BONUS, 10, 1);

        Type (String bonus, int value, int height) {
            _bonus = bonus;
            _value = value;
            _height = height;
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

        protected String _bonus;
        protected int _height;
        protected int _value;
    }

    /** The identifier for the type of effect that we produce. */
    public static final String TOTEM_ADDED =
        "bonuses/indian_post/totem/added";

    /** The normal totem pieces. */
    public static final String[] TOTEM_PIECES = {
        TOTEM_MIDDLE_BONUS, TOTEM_MEDIUM_BONUS, TOTEM_SMALL_BONUS
    };

    public static final HashMap<String, Type> TOTEM_LOOKUP = 
        new HashMap<String, Type>();

    static {
        TOTEM_LOOKUP.put(TOTEM_MIDDLE_BONUS, Type.TOTEM_MIDDLE);
        TOTEM_LOOKUP.put(TOTEM_MEDIUM_BONUS, Type.TOTEM_MEDIUM);
        TOTEM_LOOKUP.put(TOTEM_SMALL_BONUS, Type.TOTEM_SMALL);
    }

    /** The id of the totem base involved in this totem transfer or -1 if
     * we're dealing for board based totems. */
    public int baseId = -1;

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] affected = super.getAffectedPieces();
        if (drop == null) {
            return ArrayUtil.append(affected, baseId);
        }
        return affected;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (baseId > 0 && unit != null) {
            TotemBase base = (TotemBase)bangobj.pieces.get(baseId);
            if (dropping) {
                base.addPiece(type, unit.owner);
                reportEffect(obs, base, TOTEM_ADDED);
            }
        }
        return true;
    }
}
