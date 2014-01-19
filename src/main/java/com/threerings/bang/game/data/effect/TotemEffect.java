//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ListUtil;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Effects that deal with totem pieces.
 */
public class TotemEffect extends HoldEffect
{
    /** The bonus identifier for the large totem middle piece. */
    public static final String TOTEM_LARGE_BONUS = "indian_post/totem_large";
    
    /** The bonus identifier for the medium totem middle piece. */
    public static final String TOTEM_MEDIUM_BONUS = "indian_post/totem_medium";
    
    /** The bonus identifier for the small totem middle piece. */
    public static final String TOTEM_SMALL_BONUS = "indian_post/totem_small";

    /** The bonus identifier for the totem crown piece. */
    public static final String TOTEM_CROWN_BONUS = "indian_post/totem_crown";

    /** Indicates that we picked up a totem. */
    public static final String PICKED_UP_TOTEM = "indian_post/totem/pickedup";
        
    /** Indicates that we picked up a totem crown. */
    public static final String PICKED_UP_CROWN =
        "indian_post/totem_crown/pickedup";
    
    /** The identifier for the type of effect that we produce. */
    public static final String TOTEM_ADDED = "indian_post/totem/added";

    /** Indicates that we crowned the totem pole. */
    public static final String CROWN_ADDED = "indian_post/totem_crown/added";
    
    /** The normal totem pieces. */
    public static final String[] TOTEM_PIECES = {
        TOTEM_LARGE_BONUS, TOTEM_MEDIUM_BONUS, TOTEM_SMALL_BONUS
    };

    /** The id of the totem base involved in this totem transfer or -1 if
     * we're dealing for board based totems. */
    public int baseId = -1;

    /**
     * Determines whether the given bonus type represents a totem piece.
     */
    public static boolean isTotemBonus (String type)
    {
        return (type != null && (type.equals(TOTEM_CROWN_BONUS) ||
            ListUtil.contains(TOTEM_PIECES, type)));
    }
    
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
                base.addPiece(bangobj, type, unit.owner);
                reportEffect(obs, base, TOTEM_CROWN_BONUS.equals(type) ?
                    CROWN_ADDED : TOTEM_ADDED);
            }
        }
        return true;
    }
    
    @Override // documentation inherited
    public String getPickedUpEffect ()
    {
        return TOTEM_CROWN_BONUS.equals(type) ?
            PICKED_UP_CROWN : PICKED_UP_TOTEM;
    }
}
