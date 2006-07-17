//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.Iterator;
import java.util.List;

import java.awt.Point;

import com.jme.math.Vector3f;

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
        boolean walking = path.size() <= 4;
        if (walking) {
            for (Iterator iter = path.iterator(); iter.hasNext(); ) {
                Point p = (Point)iter.next();
                if (!board.isTraversable(p.x, p.y)) {
                    walking = false;
                    break;
                }
            }
        }
        setMoveType(walking ? MOVE_WALKING : MOVE_FLYING);
        return super.createPath(board, path, speed);
    }

    @Override // documentation inherited
    protected void setCoord (BangBoard board, Vector3f[] coords, int idx,
                             int nx, int ny, boolean moving)
    {
        if (MOVE_WALKING.equals(getMoveType())) {
            moving = false;
        }
        super.setCoord(board, coords, idx, nx, ny, moving);
    }
}
