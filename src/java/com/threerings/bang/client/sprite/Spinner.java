//
// $Id$

package com.threerings.bang.client.sprite;

import com.jme.math.Quaternion;
import com.jme.scene.Controller;

import com.threerings.jme.sprite.Sprite;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Spins a sprite around the up vector. Used for bonuses.
 */
public class Spinner extends Controller
{
    /**
     * Creates a spinner that rotates at the specified number of radians
     * per second.
     */
    public Spinner (Sprite sprite, float speed)
    {
        _sprite = sprite;
        _speed = speed;
    }

    // documentation inherited
    public void update (float time)
    {
        _angle += _speed * time;
        _rotation.fromAngleAxis(_angle, UP);
        _sprite.getLocalRotation().set(_rotation);
    }

    protected Sprite _sprite;
    protected float _angle, _speed;
    protected Quaternion _rotation = new Quaternion();
}
