//
// $Id$

package com.threerings.bang.game.data.effect;

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
    public static final String BONUS_POINT = "frontier_town/bonus_point";

    @Override // from Effect
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // from Effect
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing target for bonus point effect", "id", pieceId);
            return false;
        }
        reportEffect(obs, piece, BONUS_POINT);
        return true;
    }

    @Override // from Effect
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx || pidx == -1) {
            return null;
        }
        return MessageBundle.compose("m.effect_bonus_point",
            piece.getName(), MessageBundle.taint(BONUS_POINTS));
    }

    @Override // from BonusEffect
    public int getBonusPoints ()
    {
        return BONUS_POINTS;
    }

    protected static final int BONUS_POINTS = 25;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, out);
    }
}
