//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import com.samskivert.util.ArrayUtil;
 
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Effects that deal with totem pieces.
 */
public class TotemEffect extends HoldEffect
{
    /** The bonus identifier for the totem middle piece. */
    public static final String TOTEM_MIDDLE_BONUS = "indian_post/totem_middle";

    /** The bonus identifier for the totem crown piece. */
    public static final String TOTEM_CROWN_BONUS = "indian_post/totem_crown";

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

        // TODO: interact with totem bases
        return true;
    }
}
