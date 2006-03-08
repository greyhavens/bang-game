//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;

import com.threerings.jme.sprite.LineSegmentPath;
import com.threerings.jme.sprite.PathUtil;

import com.threerings.bang.client.Config;
import com.threerings.bang.client.Model;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles unit movement and does all the complicated extra business of
 * switching between actions at the proper points along the path.
 */
public class MoveUnitPath extends LineSegmentPath
{
    public MoveUnitPath (
        MobileSprite sprite, Vector3f[] coords, float[] durations)
    {
        super(sprite, UP, FORWARD, coords, durations);

        // either we do "walking" the whole time, or we break our path
        // down into "start", "cycle" and "end"
        if (sprite.hasAction("walking_start")) {
            float total = 0;
            for (int ii = 0; ii < durations.length; ii++) {
                total += durations[ii];
            }
            _actions = new String[] {
                "walking_start", "walking_cycle", "walking_end" };
            _times = new float[_actions.length];
            Model.Animation start = sprite.getAction("walking_start");
            if (start != null) {
                _times[0] = start.getDuration() / Config.display.animationSpeed;
                total -= _times[0];
            }
            Model.Animation end = sprite.getAction("walking_end");
            if (end != null) {
                _times[2] = end.getDuration() / Config.display.animationSpeed;
                total -= _times[2];
            }
            _times[1] = total + _times[0];
            _times[2] += _times[1];
            sprite.setAction("walking_start");

        } else {
            sprite.setAction("walking");
        }
        sprite.setAnimationActive(true);
    }

    @Override // documentation inherited
    public void update (float time)
    {
        super.update(time);
        if (_current >= _durations.length) {
            return;
        }
        
        // see if we're turning a corner
        int half = _accum < _durations[_current]*0.5f ?
            FIRST_HALF : SECOND_HALF;
        if (_pivots[half] != null) {
            float angle = _startAngles[half] + _angularVels[half] *
                (_accum / _durations[_current]);
            _temp.set(CORNERING_RADIUS * FastMath.cos(angle),
                CORNERING_RADIUS * FastMath.sin(angle), 0f);
            _temp.addLocal(_pivots[half]);
            _sprite.setLocalTranslation(_temp);
            _rotate.fromAngleNormalAxis(_angularVels[half] < 0 ?
                angle : angle + FastMath.PI, Vector3f.UNIT_Z);
            _sprite.setLocalRotation(_rotate);
        }
        
        // adjust to the terrain at the current coordinates
        MobileSprite sprite = (MobileSprite)_sprite;
        sprite.pathUpdate();
        
        _elapsed += time;
        if (_actions != null && _elapsed > _times[_index] &&
            _index < _actions.length-1) {
            sprite.setAction(_actions[++_index]);
            sprite.setAnimationActive(true);
        }
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();

        // restore the sprite to standing
        MobileSprite sprite = (MobileSprite)_sprite;
        sprite.pathUpdate();
        sprite.setAction(sprite.getRestPose());
        sprite.setAnimationActive(false);
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
