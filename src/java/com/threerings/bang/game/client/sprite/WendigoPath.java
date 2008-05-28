//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

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

        adjustRotation();

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
