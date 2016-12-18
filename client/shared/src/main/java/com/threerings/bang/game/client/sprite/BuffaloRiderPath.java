//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Vector3f;

import com.threerings.bang.client.Config;

import com.threerings.bang.game.client.MoveShootHandler;

import com.threerings.jme.model.Model;

/**
 * Handles a buffalo rider ramming movement.
 */
public class BuffaloRiderPath extends MoveUnitPath
{
    public BuffaloRiderPath (MobileSprite sprite, Vector3f[] coords,
            float[] durations, String type, String action, 
            MoveShootHandler handler)
    {
        super(sprite, coords, durations, type, action);
        _handler = handler;
        String type_end = "shooting_run";
        Model.Animation end = sprite.getAction(type_end);
        _ramTime = 0f;
        // replace the ending animation with the ramming animation
        if (end != null) {
            _ramTime = end.getDuration() / Config.animationSpeed;
            _times[2] -= _times[1];
            _durations[_durations.length - 1] = _ramTime - _times[2];
            _actions[2] = type_end;
            _times[2] = _ramTime + _times[1];
        }
    }

    @Override // documentation inherited
    public void update (float time)
    {
        super.update(time);
        // Have the shot go off once we start the ramming animation 
        if (_handler != null && 
                (_elapsed > _times[1] || _current >= _durations.length)) {
            _handler.fireShot();
            _handler = null;
            ((UnitSprite)_sprite).setShootHandler(null);
        }
    }

    @Override // documentation inherited
    public void updateRotation ()
    {
        super.updateRotation();
        if (_current + 1 >= _durations.length) {
            // once we've generated the rotation information, make sure
            // the sprite doesn't move on the target's tile
            _points[_points.length - 1] = _points[_points.length - 2];
            _translateSprite = false;
        }
    }

    @Override // documentation inherited
    public void updateSpriteRotation ()
    {
        super.updateSpriteRotation();
        if (!_translateSprite) {
            _sprite.setLocalTranslation(_points[_points.length - 1]);
        }
    }

    /** Reference to the handler. */
    MoveShootHandler _handler;

    /** Time it takes to complete the ramming animation. */
    float _ramTime;

    boolean _translateSprite = true;
}
