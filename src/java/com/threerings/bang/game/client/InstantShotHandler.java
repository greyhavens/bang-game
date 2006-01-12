//
// $Id$

package com.threerings.bang.game.client;

/**
 * Waits for all sprites involved in a shot to stop moving and then animates
 * the fired shot.
 */
public class InstantShotHandler extends ShotHandler
{
    protected void fireShot (int sx, int sy, int tx, int ty)
    {
        // play the bang sound
        if (_bangSound != null) {
            _bangSound.play(false);
        }

        // apply the effect and complete our handling if that did not result in
        // anything that needs waiting for
        _effect.apply(_bangobj, this);
        maybeComplete(-1);
    }
}
