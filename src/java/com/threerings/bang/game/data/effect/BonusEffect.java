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
    /** The generic bonus activation effect. */
    public static final String ACTIVATED_BONUS =
        "effects/frontier_town/get_bonus";
    
    /** The id of the bonus piece that triggered this effect. */
    public int bonusId = -1;

    /** The id of the piece that activated the bonus. */
    public int pieceId = -1;
    
    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }
    
    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return new int[] { pieceId };
    }
    
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
                String effect = getActivatedEffect();
                if (effect != null) {
                    reportEffect(obs, bonus, effect);
                }
                bangobj.removePieceDirect(bonus);
                reportRemoval(obs, bonus);
            }
        }
        return true;
    }
    
    /**
     * Returns the name of the effect to report on the bonus when it is removed
     * from the board, or <code>null</code> for none.
     */
    protected String getActivatedEffect ()
    {
        return ACTIVATED_BONUS;
    }
}
