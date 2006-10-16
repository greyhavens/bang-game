//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.effect.RobotWaveEffect;

/**
 * Handles the effects generated at the beginning and end of waves of robots.
 */
public class RobotWaveHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        super.execute();
        RobotWaveEffect reffect = (RobotWaveEffect)_effect;
        if (reffect.wave > 0) {
            displayStartMarquee(reffect.wave, reffect.difficulty);
        } else {
            displayEndMarquee(reffect.living, reffect.treeIds.length);
        }
        return true;
    }
    
    /**
     * Displays the marquee indicating the start of the wave.
     */
    protected void displayStartMarquee (int wave, int difficulty)
    {
        String msg;
        if (wave < 10) {
            msg = "m.nth." + wave;
        } else {
            msg = MessageBundle.compose("m.nth.ten_plus", wave);
        }
        _view.fadeMarqueeInOut(MessageBundle.compose("m.wave_start", msg,
            MessageBundle.compose("m.stars", difficulty + 1)), 1f);
        notePender(2f);
    }
    
    /**
     * Displays the marquee indicating the end of the wave.
     */
    protected void displayEndMarquee (int living, int total)
    {
        _view.fadeMarqueeInOut(MessageBundle.tcompose(
            "m.wave_end", living, total), 1f);
        notePender(2f);
    }
}
