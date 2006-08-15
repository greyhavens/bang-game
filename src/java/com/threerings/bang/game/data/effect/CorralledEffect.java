//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Applied to a cow that has been corralled.
 */
public class CorralledEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String CORRALLED = "frontier_town/corralled";

    /** The piece we will affect. */
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
        // nothing doing
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        // for now just remove the cow in question
        Cow cow = (Cow)bangobj.pieces.get(pieceId);
        if (cow == null) {
            log.warning("Missing cow for corral effect [id=" + pieceId + "].");
            return false;
        }

        // mark the cow as corralled
        cow.corralled = true;

        // TEMP: just remove the cow
        bangobj.removePieceDirect(cow);
        reportRemoval(obs, cow);

        return true;
    }
}
