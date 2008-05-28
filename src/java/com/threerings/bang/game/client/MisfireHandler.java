//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.client.sprite.ActiveSprite;
import com.threerings.bang.game.client.sprite.GunshotEmission;
import com.threerings.bang.game.client.sprite.MisfireEmission;
import com.threerings.bang.game.client.sprite.MobileSprite;

import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * Waits for all the sprites involved in a shot to stop moving and then
 * animates the misfire.
 */
public class MisfireHandler extends InstantShotHandler
{
    public MisfireHandler (int animType) {
        _animType = animType;
    }

    @Override // documentation inherited
    protected void animateShooter (MobileSprite sprite)
    {
        sprite.addActionHandler(new ActiveSprite.ActionHandler() {
            public String handleAction (ActiveSprite sprite, String action) {
                if (!ShotEffect.SHOT_ACTIONS[_shot.type].equals(action)) {
                    return action;
                }
                // Turn off normal gunshot emissions and activate
                // dud shot emissions
                for (Object ctrl : sprite.getModelControllers()) {
                    if (ctrl instanceof GunshotEmission) {
                        ((GunshotEmission)ctrl).setActiveEmission(false);
                    } else if (ctrl instanceof MisfireEmission) {
                        ((MisfireEmission)ctrl).setActiveEmission(true);
                    }
                }
                sprite.removeActionHandler(this);
                return ShotEffect.SHOT_ACTIONS[_animType];
            }
        });
        super.animateShooter(sprite);
    }

    /** The shot type for the animation. */
    protected int _animType;
}
