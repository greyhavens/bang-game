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
    /** An effect reported when the bonus is activated. */
    public static final String ACTIVATED = "activated";
    
    /** The id of the bonus piece that triggered this effect. */
    public int bonusId = -1;

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        if (bonusId > 0) {
            // remove the bonus from the board
            Piece bonus = bangobj.pieces.get(bonusId);
            if (bonus == null) {
                log.warning("Missing bonus for activated effect? " +
                            "[id=" + bonusId + "].");
            } else {
                reportEffect(obs, bonus, ACTIVATED);
                bangobj.removePieceDirect(bonus);
                reportRemoval(obs, bonus);
            }
        }
        return true;
    }
}
