//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.FadeBoardHandler;

import com.threerings.bang.game.data.BangObject;

/**
 * Fades the ambient lighting on the board.
 */
public class FadeBoardEffect extends Effect
{
    /** The name of our board effect. */
    public static final String WENDIGO_AMBIANCE =
        "indian_post/wendigo_ambiance";

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return NO_PIECES;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // nothing doing
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        affectBoard(bangobj, WENDIGO_AMBIANCE, false, obs);
        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new FadeBoardHandler();
    }
}
