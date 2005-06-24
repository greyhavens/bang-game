//
// $Id$

package com.threerings.bang.client;

/**
 * Waits for all sprites involved in a shot to stop moving and then
 * animates the fired shot.
 */
public class InstantShotHandler extends ShotHandler
{
    protected void fireShot ()
    {
        // TODO: activate the "firing" animation on the shooter
        _view.applyShot(_shot);
    }
}
