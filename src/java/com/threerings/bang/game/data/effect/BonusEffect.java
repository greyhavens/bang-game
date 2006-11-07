//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Represents the effect of a piece activating a bonus.
 */
public abstract class BonusEffect extends Effect
{
    /** The generic bonus activation effect. */
    public static final String ACTIVATED_BONUS = "frontier_town/get_bonus";

    /** We grant these points to a player that picks up a bonus. */
    public static final int DEFAULT_BONUS_POINTS = 10;

    /** The id of the bonus piece that triggered this effect. */
    public int bonusId = -1;

    /** The id of the piece that activated the bonus. */
    public int pieceId = -1;

    /** In case something goes wrong, we can always punt. */
    public transient PuntEffect puntEffect;

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
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // if this effect was really triggered by activating a bonus...
        if (bonusId != -1) {
            Piece piece = bangobj.pieces.get(pieceId);
            int points = getBonusPoints();
            if (piece == null || piece.owner == -1 || points == 0) {
                return;
            }
            // ...grant points to the activating player
            bangobj.grantBonusPoints(piece.owner, points);
        }
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
                removeAndReport(bangobj, bonus, obs);
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

    /**
     * Returns the number of points granted to the player that picks up this
     * bonus.
     */
    protected int getBonusPoints ()
    {
        return DEFAULT_BONUS_POINTS;
    }
}
