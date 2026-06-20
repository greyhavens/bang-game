//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import com.threerings.jme.sprite.CubicSplinePath;
import com.threerings.jme.sprite.PathUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles the movement of a wendigo.
 */
public class WendigoPath extends CubicSplinePath
{
    public WendigoPath (
            MobileSprite sprite, Vector3f[] coords, float[] durations)
    {
        super(sprite, coords, durations);

        for (float d : durations) {
            _outFadeTime += d;
        }
        _outFadeTime -= FADE_DURATION;

        sprite.setAction("idle");
        ((WendigoSprite)sprite).fade(true, FADE_DURATION);
    }

    @Override // documentation inherited
    public void update (float time)
    {
        super.update(time);

        // super.update() may have completed the path -- advancing _current to
        // _durations.length and calling pathCompleted() -- in which case interpolate()
        // would index _durations[_current] out of bounds (ArrayIndexOutOfBounds on the
        // jME3 render thread, which kills the client). jME1 stopped ticking the path once
        // it completed; jME3 still runs the rest of this override, so only adjust the
        // rotation while the path is still active.
        if (_current < _durations.length) {
            adjustRotation();
        }

        _elapsed += time;
        if (_elapsed > _outFadeTime) {
            ((WendigoSprite)_sprite).fade(false, FADE_DURATION);
            _outFadeTime = Float.MAX_VALUE;
        }
    }

    /**
     * Adjust the sprite rotation based on the path.
     */
    protected void adjustRotation ()
    {
        Vector3f pos = interpolate(-0.05f),
                 pos2 = interpolate(0.05f).subtract(pos);
        pos.set(FORWARD);
        pos.z = FastMath.invSqrt(FastMath.sqr(pos2.x) + FastMath.sqr(pos2.y)) *
            pos2.z/2;
        pos2.z = 0;
        Quaternion rot = new Quaternion(), tilt = new Quaternion();
        PathUtil.computeRotation(UP, FORWARD, pos2, rot);
        PathUtil.computeRotation(LEFT, FORWARD, pos, tilt);
        _sprite.setLocalRotation(rot.multLocal(tilt));
    }

    protected float _outFadeTime, _elapsed;

    protected static final float FADE_DURATION = 0.5f;
}
