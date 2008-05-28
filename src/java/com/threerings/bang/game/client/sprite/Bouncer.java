//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;

/**
 * Bounces a sprite (or any spatial) up and down along the Z axis. Used for the
 * cursor.
 */
public class Bouncer extends Controller
{
    /**
     * Creates a bouncer that bounces using a sine function at the specified
     * number of radians per second, with a range of the specified distance.
     */
    public Bouncer (Spatial target, float speed, float range)
    {
        _target = target;
        _speed = speed;
        _range = range;
    }

    // documentation inherited
    public void update (float time)
    {
        _target.getLocalTranslation().z -= _dz;
        _angle += _speed * time;
        _dz = FastMath.sin(_angle) * _range;
        _target.getLocalTranslation().z += _dz;
    }

    protected Spatial _target;
    protected float _angle, _dz;
    protected float _speed, _range;
}
