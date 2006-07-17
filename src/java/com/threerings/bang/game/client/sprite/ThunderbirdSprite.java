//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Vector3f;

import com.threerings.jme.sprite.LinePath;


/**
 * Sprite for the Thunderbird unit.
 */
public class ThunderbirdSprite extends UnitSprite
{
    public ThunderbirdSprite (String type)
    {
        super(type);
    }

    @Override // documentation inherited
    protected void startNextAction ()
    {
        super.startNextAction();
        // If we're dying, drop us to the ground while playing the dying
        // animation
        if (_action == "dying") {
            Vector3f air = new Vector3f(localTranslation),
                     ground = toWorldCoords(_piece.x, _piece.y,
                        _piece.computeElevation(_view.getBoard(),
                            _piece.x, _piece.y, false), new Vector3f());
            move(new LinePath(this, air, ground, _nextAction));
        }
    }
}
