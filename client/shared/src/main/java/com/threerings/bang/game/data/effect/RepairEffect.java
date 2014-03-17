//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * An effect that repairs a particular piece on the board.
 */
public class RepairEffect extends BonusEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String REPAIRED = "frontier_town/repair";

    /** The identifier for the type of effect that we produce. */
    public static final String HALF_REPAIRED = "frontier_town/half_repair";
    
    /** The base amount by which to repair the piece. */
    public int baseRepair = 100;

    /** The updated damage for the affected piece. */
    public int newDamage;

    /**
     * Determines whether the given piece effect is one of the repair effects.
     */
    public static boolean isRepairEffect (String effect)
    {
        return REPAIRED.equals(effect) || HALF_REPAIRED.equals(effect);
    }
    
    /**
     * The constructor used when we're created by a bonus.
     */
    public RepairEffect ()
    {
    }

    /**
     * The constructor used when we're created by a card. Note, in this case
     * {@link #init} will not be called.
     */
    public RepairEffect (int pieceId)
    {
        this.pieceId = pieceId;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);

        // compute the new total damage for the affected piece
        Piece target = bangobj.pieces.get(pieceId);
        if (target != null) {
            newDamage = Math.max(0, target.damage - baseRepair);
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing target for repair effect", "id", pieceId);
            return false;
        }

        piece.damage = newDamage;
        reportEffect(obs, piece, baseRepair == 100 ? REPAIRED : HALF_REPAIRED);
        return true;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx || pidx == -1) {
            return null;
        }
        return MessageBundle.compose("m.effect_repair", piece.getName());
    }

    @Override // documentation inherited
    protected String getActivatedEffect ()
    {
        return null;
    }
}
