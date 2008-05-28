//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;

import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.LineSegmentPath;
import com.threerings.jme.sprite.PathUtil;

import com.threerings.bang.client.Config;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles unit movement and does all the complicated extra business of
 * switching between actions at the proper points along the path.
 */
public class MoveUnitPath extends LineSegmentPath
{
    public MoveUnitPath (MobileSprite sprite, Vector3f[] coords, float[] durations)
    {
        this(sprite, coords, durations, "walking", null);
    }

    public MoveUnitPath (MobileSprite sprite, Vector3f[] coords, float[] durations,
                         String type, String action)
    {
        super(sprite, UP, FORWARD, coords, durations);

        // either we do _type_ the whole time, or we break our path down into "start", "cycle" and
        // "end"
        String type_start = type + "_start";
        String type_cycle = type + "_cycle";
        String type_end = type + "_end";
        if (action != null) {
            sprite.setAction(action);

        } else if (sprite.hasAction(type_start)) {
            float total = 0;
            for (int ii = 0; ii < durations.length; ii++) {
                total += durations[ii];
            }
            _actions = new String[] { type_start,  type_cycle, type_end };
            _times = new float[_actions.length];
            Model.Animation start = sprite.getAction(type_start);
            if (start != null) {
                _times[0] = start.getDuration() / Config.animationSpeed;
                total -= _times[0];
            }
            Model.Animation end = sprite.getAction(type_end);
            if (end != null) {
                _times[2] = end.getDuration() / Config.animationSpeed;
                total -= _times[2];
            }
            _times[1] = total + _times[0];
            _times[2] += _times[1];
            sprite.setAction(type_start);

        } else {
            sprite.setAction(type);
        }
    }

    @Override // documentation inherited
    public void update (float time)
    {
        super.update(time);
        if (_current >= _durations.length) {
            return;
        }

        updateSpriteRotation();

        // adjust to the terrain at the current coordinates
        MobileSprite sprite = (MobileSprite)_sprite;
        sprite.pathUpdate();

        _elapsed += time;
        if (_actions != null && _elapsed > _times[_index] && _index < _actions.length-1) {
            // now switch to the next action
            sprite.setAction(_actions[++_index]);
        }
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();

        // start the sprite's pending action or make it idle
        MobileSprite sprite = (MobileSprite)_sprite;
        sprite.pathUpdate();
        sprite.startNext();
    }

    @Override // documentation inherited
    protected void updateRotation ()
    {
        _points[_current+1].subtract(_points[_current], _temp);
        _temp.z = 0f;
        PathUtil.computeRotation(_up, _orient, _temp, _rotate);
        _sprite.setLocalRotation(_rotate);

        updateCorneringParams(FIRST_HALF);
        updateCorneringParams(SECOND_HALF);
    }

    /**
     * Handles rotating the sprite around turns.
     */
    protected void updateSpriteRotation ()
    {
        // see if we're turning a corner
        int half = _accum < _durations[_current]*0.5f ? FIRST_HALF : SECOND_HALF;
        if (_pivots[half] != null) {
            float angle = _startAngles[half] + _angularVels[half] * (_accum / _durations[_current]);
            _temp.set(CORNERING_RADIUS * FastMath.cos(angle),
                      CORNERING_RADIUS * FastMath.sin(angle), 0f);
            _temp.addLocal(_pivots[half]);
            _sprite.setLocalTranslation(_temp);
            _rotate.fromAngleNormalAxis(
                _angularVels[half] < 0 ? angle : angle + FastMath.PI, Vector3f.UNIT_Z);
            _sprite.setLocalRotation(_rotate);
        }
    }

    /**
     * Updates the cornering parameters for the first or second half of the
     * current leg.
     */
    protected void updateCorneringParams (int half)
    {
        if (_pivots == null) {
            _pivots = new Vector3f[2];
            _startAngles = new float[2];
            _angularVels = new float[2];
        }

        int idx = _current + half;
        if (idx <= 0 || idx >= _points.length - 1) {
            _pivots[half] = null;
            return;
        }
        Vector3f v1 = _points[idx].subtract(_points[idx-1]),
            v2 = _points[idx+1].subtract(_points[idx]);
        if (FastMath.abs(v1.x*v2.x + v1.y*v2.y) > FastMath.FLT_EPSILON) {
            _pivots[half] = null;
            return;
        }
        _pivots[half] = new Vector3f();
        _pivots[half].interpolate(_points[idx-1], _points[idx+1], 0.5f);
        _points[_current].subtract(_pivots[half], v1);
        _points[_current + 1].subtract(_pivots[half], v2);
        _startAngles[half] = FastMath.atan2(v1.y, v1.x);
        v1.z = v2.z = 0f;
        Vector3f v3 = v1.cross(v2);
        _angularVels[half] = FastMath.asin(v3.length()) * (v3.z > 0 ? +1 : -1);
    }

    protected String[] _actions;
    protected float[] _times;
    protected float _elapsed;
    protected int _index;

    /** Angular parameters for the first and second half of the current leg. */
    protected Vector3f[] _pivots;
    protected float[] _startAngles, _angularVels;

    protected static final int FIRST_HALF = 0;
    protected static final int SECOND_HALF = 1;

    protected static final float CORNERING_RADIUS = TILE_SIZE * 0.5f;
}
