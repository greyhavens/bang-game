//
// $Id$

package com.threerings.bang.game.client.effect;

import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

/**
 * Plays the sound that goes along with an effect.
 */
public class PlaySoundViz extends EffectViz
{
    public PlaySoundViz (SoundGroup group, String path)
    {
        _sound = group.getSound(path);
    }

    @Override // documentation inherited
    public void display ()
    {
        // TODO: set the location of the sound
        _sound.play(true);
    }

    protected Sound _sound;
}
