//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.LassoBonusEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Bonus;

/**
 * Allows players to turn bonuses on the board into cards.
 */
public class Lasso extends AreaCard
{
    @Override // documentation inherited
    public String getType ()
    {
        return "lasso";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 0;
    }

    @Override // documentation inherited
    public boolean isValidLocation (BangObject bangobj, int tx, int ty)
    {
        if (!bangobj.board.containsBonus(tx, ty)) {
            return false;
        }
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Bonus && piece.intersects(tx, ty) &&
                !((Bonus)piece).getConfig().hidden && ((Bonus)piece).getConfig().cardType != null) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 60;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 20;
    }

    @Override // documentation inherited
    public Badge.Type getQualifier ()
    {
        return Badge.Type.BONUSES_COLLECTED_1;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        return new LassoBonusEffect(owner, coords[0], coords[1]);
    }
}
