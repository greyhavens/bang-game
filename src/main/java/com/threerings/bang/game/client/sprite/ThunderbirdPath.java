//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Vector3f;

import com.threerings.bang.client.Config;

import com.threerings.bang.game.client.MoveShootHandler;

import com.threerings.jme.model.Model;

/**
 * Handles a thunderbird attack movement.
 */
public class ThunderbirdPath extends MoveUnitPath
{
    public ThunderbirdPath (MobileSprite sprite, Vector3f[] coords,
            float[] durations, String type, String action, 
            MoveShootHandler handler, int attackIdx)
    {
        super(sprite, coords, durations, type, action);
        _handler = handler;
        Model.Animation shooting = sprite.getAction("shooting");
        if (shooting != null) {
            _attackIdx = attackIdx;
            _type = type;
            float shootingDuration = shooting.getDuration() / 
                Config.animationSpeed;
            float oldDuration = durations[attackIdx - 1] + durations[attackIdx];
            durations[attackIdx - 1] = durations[attackIdx] = 
                shootingDuration / 2f;
            _times[1] += shootingDuration - oldDuration;
            _times[2] += shootingDuration - oldDuration;
            // If we're attacking the next tile, jump right into the
            // attack animation
            if (_attackIdx == 1) {
                if (_times != null) {
                    _times[0] = 0;
                    _index++;
                }
                _attacking = true;
                sprite.setAction("shooting");
            }
        }
    }

    @Override // documentation inherited
    public void update (float time)
    {
        super.update(time);
        // Have the shot go off when we're on the target index
        if (_handler != null && _current >= _attackIdx) {
            _handler.fireShot();
            _handler = null;
        }

        // Have the attack animation play in the middle
        MobileSprite sprite = (MobileSprite)_sprite;
        if (_current > _attackIdx) {
            if (_attacking) { 
                if (_actions == null) {
                    sprite.setAction(_type);
                } else if (_index < _actions.length - 1) {
                    sprite.setAction(_actions[_index]);
                }
                _attacking = false;
            }
        } else if (_current >= _attackIdx - 1) {
            if (!_attacking) {
                sprite.setAction("shooting");
                _attacking = true;
            }
        }
    }

    /** The path segment that is the attacking the target. */
    protected int _attackIdx = Integer.MAX_VALUE;

    /** Reference to our handler. */
    protected MoveShootHandler _handler;

    /** If we're in the attack animation. */
    protected boolean _attacking = false;

    /** Type of movement animation we're doing. */
    protected String _type;
}
