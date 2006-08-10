//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Clears all the modifications.
 */
public class ClearAllModificationsEffect extends GlobalEffect
{
    @Override // documentation inherited
    protected boolean validPiece (Piece piece)
    {
        return (piece instanceof Unit && (((Unit)piece).influence != null ||
                    ((Unit)piece).hindrance != null));
    }

    @Override // documentation inherited
    protected Effect getEffect (Piece piece)
    {
        ClearModificationsEffect effect = new ClearModificationsEffect();
        effect.init(piece);
        return effect;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        return "m.effect_clear_all_modifications";
    }
}
