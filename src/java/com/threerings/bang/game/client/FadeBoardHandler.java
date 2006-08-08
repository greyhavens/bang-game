//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.data.effect.FadeBoardEffect;

/**
 * Handles a fade board effect on the client.
 */
public class FadeBoardHandler extends EffectHandler
{
    @Override // documentation inherited
    public void boardAffected (String effect)
    {
        if (FadeBoardEffect.WENDIGO_AMBIANCE.equals(effect)) {
            _view.setWendigoAmbiance(WENDIGO_FADE_DURATION, true);
        } else {
            super.boardAffected(effect);
        }
    }

    /** Time it takes to fade the board. */
    protected static final float WENDIGO_FADE_DURATION = 0.8f;
}
