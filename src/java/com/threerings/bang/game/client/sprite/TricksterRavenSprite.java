//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.List;

import com.threerings.bang.game.data.BangBoard;

import com.threerings.jme.sprite.Path;

/**
 * Sprite for the Trickster Raven unit.
 */
public class TricksterRavenSprite extends UnitSprite
{
    public TricksterRavenSprite (String type)
    {
        super(type);
    }

    @Override // documentation inherited
    protected Path createPath (BangBoard board, List path, float speed)
    {
        setMoveType((path.size() > 2) ? MOVE_FLYING : MOVE_WALKING);
        return super.createPath(board, path, speed);
    }
}
