//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.Arrays;

import com.jme.math.Vector3f;

import com.threerings.jme.sprite.LineSegmentPath;

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

        // figure out which actions we'll use all along the way
        _actions = new String[durations.length];

        // fill in our standard walking cycle
        Arrays.fill(_actions, sprite.hasAction("walking_cycle") ?
                    "walking_cycle" : "walking");

        // add a start and end animation if we have them
        if (_actions.length > 1) {
            if (sprite.hasAction("walking_start")) {
                _actions[0] = "walking_start";
            }
            if (sprite.hasAction("walking_end")) {
                _actions[_actions.length-1] = "walking_end";
            }
        }

        // start with our first action
        sprite.setAction(_actions[0]);
        sprite.setAnimationActive(true);
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

    @Override // documentation inherited
    protected void advance ()
    {
        super.advance();

        // stop now if we're about to finish the path
        if (_current == _actions.length) {
            return;
        }

        // update our current animation action if it changed
        if (!_actions[_current].equals(_actions[_current-1])) {
            MobileSprite sprite = (MobileSprite)_sprite;
            sprite.setAction(_actions[_current]);
            sprite.setAnimationActive(true);
        }
    }

    protected String[] _actions;
}
