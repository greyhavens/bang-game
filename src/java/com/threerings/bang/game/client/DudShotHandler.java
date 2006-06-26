//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.client.sprite.DudShotEmission;
import com.threerings.bang.game.client.sprite.GunshotEmission;
import com.threerings.bang.game.client.sprite.MobileSprite;

import com.threerings.bang.game.data.effect.ShotEffect;

import static com.threerings.bang.Log.log;

/**
 * Waits for all the sprites involved in a shot to stop moving and then
 * animates the dud shot.
 */
public class DudShotHandler extends InstantShotHandler
{
    protected void animateShooter (MobileSprite sprite)
    {
        sprite.addActionHandler(new MobileSprite.ActionHandler() {
            public String handleAction (MobileSprite sprite, String action) {
                if (!ShotEffect.SHOT_ACTIONS[_shot.type].equals(action)) {
                    return action;
                }
                // Turn off normal gunshot emissions and activate
                // dud shot emissions
                for (Object ctrl : sprite.getModelControllers()) {
                    if (ctrl instanceof GunshotEmission) {
                        ((GunshotEmission)ctrl).setActiveEmission(false);
                    } else if (ctrl instanceof DudShotEmission) {
                        ((DudShotEmission)ctrl).setActiveEmission(true);
                    }
                }
                sprite.removeActionHandler(this);
                return ShotEffect.SHOT_ACTIONS[ShotEffect.NORMAL];
            }
        });
        super.animateShooter(sprite);
    }
}
