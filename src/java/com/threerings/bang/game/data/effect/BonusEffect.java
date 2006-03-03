//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Represents the effect of a piece activating a bonus.
 */
public abstract class BonusEffect extends Effect
{
    /** The id of the bonus piece that triggered this effect. */
    public int bonusId = -1;

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        if (bonusId > 0) {
            // remove the bonus from the board
            Piece bonus = (Piece)bangobj.pieces.get(bonusId);
            if (bonus == null) {
                log.warning("Missing bonus for activated effect? " +
                    "[id=" + bonusId + "].");
            } else {
                bangobj.removePieceDirect(bonus);
                reportRemoval(obs, bonus);
            }
        }
    }
}
