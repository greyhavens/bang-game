//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Quaternion;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Spins a sprite (or any spatial) around the up vector. Used for bonuses.
 */
public class Spinner extends Controller
{
    /**
     * Creates a spinner that rotates at the specified number of radians
     * per second.
     */
    public Spinner (Spatial target, float speed)
    {
        _target = target;
        _speed = speed;
    }

    /**
     * Sets the spinner's speed.
     */
    public void setSpeed (float speed)
    {
        _speed = speed;
    }
    
    /**
     * Returns the spinner's speed.
     */
    public float getSpeed (float speed)
    {
        return _speed;
    }
    
    // documentation inherited
    public void update (float time)
    {
        _angle += _speed * time;
        _rotation.fromAngleAxis(_angle, UP);
        _target.getLocalRotation().set(_rotation);
    }

    protected Spatial _target;
    protected float _angle, _speed;
    protected Quaternion _rotation = new Quaternion();
}
