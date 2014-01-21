//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.ArrayList;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Performs an effect on some set of pieces on the board.
 */
public abstract class GlobalEffect extends BonusEffect
{
    /** The set of effects. */
    public Effect[] effects = new Effect[0];

    // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] pieces = new int[0];
        for (Effect effect : effects) {
            pieces = concatenate(pieces, effect.getAffectedPieces());
        }
        return pieces;
    }

    // documentation inherited
    public int[] getWaitPieces ()
    {
        int[] pieces = super.getWaitPieces();
        for (Effect effect : effects) {
            pieces = concatenate(pieces, effect.getWaitPieces());
        }
        return pieces;
    }

    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {

        ArrayList<Effect> effectList = new ArrayList<Effect>();
        for (Piece piece : bangobj.pieces) {
            if (validPiece(piece)) {
                Effect effect = getEffect(piece);
                effect.prepare(bangobj, dammap);
                effectList.add(effect);
            }
        }
        effects = effectList.toArray(effects);

        if (isApplicable()) {
            super.prepare(bangobj, dammap);
        }
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return (effects != null && effects.length > 0);
    }

    // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);
        for (Effect effect : effects) {
            effect.apply(bangobj, obs);
        }
        return true;
    }

    /**
     * Returns true if this piece should be affected.
     */
    protected abstract boolean validPiece (Piece piece);

    /**
     * Creates the effect that will be applied to the piece.
     */
    protected abstract Effect getEffect (Piece piece);
}
