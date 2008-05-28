//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.List;

import java.awt.Point;

import com.jme.math.Vector3f;

import com.threerings.jme.sprite.Path;
import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.game.data.BangBoard;

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
    protected Path createPath (BangBoard board, List<Point> path, float speed)
    {
        boolean walking = path.size() <= 4;
        if (walking) {
            Point lastp = null;
            for (Point p : path) {
                if (!board.isWalkable(p.x, p.y, _piece) ||
                    (lastp != null && !board.canCross(lastp.x, lastp.y, p.x, p.y))) {
                    walking = false;
                    break;
                }
                lastp = p;
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
    
    @Override // documentation inherited
    protected void createSounds (SoundGroup sounds)
    {
        super.createSounds(sounds);

        // load the sounds for flying
        String path = "rsrc/units/indian_post/tricksterraven/";
        _flyStart = sounds.getSound(path + "to_raven.ogg");
        _flyLoop = sounds.getSound(path + "flying.ogg");
        _flyStop = sounds.getSound(path + "to_human.ogg");
    }
    
    @Override // documentation inherited
    protected void startMoveSound ()
    {
        if (MOVE_WALKING.equals(getMoveType())) {
            super.startMoveSound();
            return;
        }
        _flyStart.play(false);
        _flyLoop.loop(false);
    }
    
    @Override // documentation inherited
    protected void stopMoveSound ()
    {
        if (MOVE_WALKING.equals(getMoveType())) {
            super.stopMoveSound();
            return;
        }
        _flyLoop.stop();
        _flyStop.play(false);
    }
    
    /** The sounds to play at the beginning, middle, and end of flight. */
    protected Sound _flyStart, _flyLoop, _flyStop;
}
