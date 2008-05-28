//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Represents a unit's picking up or dropping a fool's nugget.
 */
public class FoolsNuggetEffect extends NuggetEffect
{
    /** The bonus type for a nugget of fool's gold. */
    public static final String FOOLS_NUGGET_BONUS =
        "frontier_town/fools_nugget";

    /** The identifier for the fool's gold nugget rejection effect. */
    public static final String FOOLS_NUGGET_REJECTED =
        "frontier_town/fools_nugget/rejected";

    public FoolsNuggetEffect ()
    {
        type = FOOLS_NUGGET_BONUS;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx || pidx == -1 || 
                claimId <= 0 || !dropping) {
            return null;
        }
        return MessageBundle.compose("m.effect_fools_gold", piece.getName());
    }

    @Override // documentation inherited
    protected void applyToClaim (BangObject bangobj, Observer obs)
    {
        reportEffect(obs, bangobj.pieces.get(claimId), FOOLS_NUGGET_REJECTED);
    }
}
