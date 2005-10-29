//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Vector3f;

import com.threerings.jme.sprite.LineSegmentPath;

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

        // adjust to the terrain at the current coordinates
        MobileSprite sprite = (MobileSprite)_sprite;
        sprite.snapToTerrain();
        
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
        sprite.setAction(sprite.getRestPose());
        sprite.setAnimationActive(false);
    }

    protected String[] _actions;
    protected float[] _times;
    protected float _elapsed;
    protected int _index;
}
