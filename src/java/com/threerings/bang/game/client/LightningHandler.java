//
// $Id$

package com.threerings.bang.game.client;

import com.samskivert.util.Interval;

import com.threerings.bang.game.data.effect.LightningEffect;

/**
 * Animates a chained lightning.
 */
public class LightningHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        _applying = true;
        _leffect = (LightningEffect)_effect;

        apply(_effect);
        final int penderId = notePender();
        new Interval(_ctx.getClient().getRunQueue()) {
            public void expired () {
                // keep applying the next level until done
                if (!_leffect.apply(
                            _bangobj, LightningHandler.this, _level++)) {
                    cancel();
                    maybeComplete(penderId);
                }
            }
            protected byte _level;
        }.schedule(CHAIN_DELAY);
        _applying = false;
        return !isCompleted();
    }

    LightningEffect _leffect;

    /** The delay in milliseconds between subsequent levels. */
    protected static final long CHAIN_DELAY = 250;
}
