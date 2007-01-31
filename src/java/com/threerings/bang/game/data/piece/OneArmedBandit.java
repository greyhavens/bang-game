//
// $Id$

package com.threerings.bang.game.data.piece;

import com.samskivert.util.ArrayUtil;

import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.effect.Effect;


/**
 * Handles some special custom behavior needed for the One Armed Jack.
 */
public class OneArmedBandit extends Unit
{
    @Override // documentation inherited
    public Effect[] maybeGeneratePostOrderEffects ()
    {
        return super.maybeGeneratePostOrderEffects();
    }
}
