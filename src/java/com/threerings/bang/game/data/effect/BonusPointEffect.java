//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Grants bonus points to the acquiring player.
 */
public class BonusPointEffect extends BonusEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String BONUS_POINT =
        "bonuses/frontier_town/bonus_point/activate";

    public int pieceId;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece piece = (Piece)bangobj.pieces.get(pieceId);
        if (piece == null) {
            return;
        }
        // grant points to the activating player
        bangobj.grantBonusPoints(piece.owner, BONUS_POINTS);
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing target for bonus point effect " +
                        "[id=" + pieceId + "].");
            return false;
        }
        reportEffect(obs, piece, BONUS_POINT);
        return true;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx) {
            return null;
        }
        return MessageBundle.compose("m.effect_bonus_point",
            piece.getName(), MessageBundle.taint(BONUS_POINTS));
    }
    
    protected static final int BONUS_POINTS = 50;
}
