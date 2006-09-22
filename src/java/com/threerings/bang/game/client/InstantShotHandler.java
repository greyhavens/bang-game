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
        // play the bang sounds
        playSounds(_bangSounds, true);

        // apply the effect and complete our handling if that did not result in
        // anything that needs waiting for
        apply(_effect);
        maybeComplete(-1);
    }
}
