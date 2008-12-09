//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.List;
import java.awt.Point;

import com.jme.math.Vector3f;

import com.samskivert.util.ListUtil;

import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.game.client.MoveShootHandler;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;

import com.threerings.jme.sprite.Path;

import static com.threerings.bang.Log.log;

/**
 * Sprite for the Buffalo Rider unit.
 */
public class BuffaloRiderSprite extends UnitSprite
{
    public BuffaloRiderSprite (String type)
    {
        super(type);
    }

    @Override // documentation inherited
    public Sound getShotSound (SoundGroup sounds, ShotEffect shot)
    {
        String path = "rsrc/units/indian_post/buffalo_rider/";
        if (shot.pushx != -1) {
            return sounds.getSound(path + "attack_push.ogg");
        } else if (ListUtil.contains(shot.attackIcons, "smashed") ||
            ListUtil.contains(shot.defendIcons, "unmovable")) {
            return sounds.getSound(path + "attack_push_blocked.ogg");
        } else {
            return super.getShotSound(sounds, shot);
        }
    }
    
    @Override // documentation inherited
    public boolean updatePosition (BangBoard board)
    {
        if (_fastAnimation && _effectHandler != null) {
            ((MoveShootHandler)_effectHandler).fireShot();
            _effectHandler = null;
        }
        return super.updatePosition(board);
    }

    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();
        if (_effectHandler != null) {
            log.warning("Buffalo Rider completed attack path without firing",
                        "effectHandler", _effectHandler);
            ((MoveShootHandler)_effectHandler).fireShot();
            _effectHandler = null;
        }
    }

    @Override // documentation inherited
    protected Path createPath (BangBoard board)
    {
        Path path = super.createPath(board);
        // something's booched so fire off the shot now
        if (path == null && _effectHandler != null) {
            ((MoveShootHandler)_effectHandler).fireShot();
            _effectHandler = null;
        }
        return path;
    }

    @Override // documentation inherited
    protected Path createPath (BangBoard board, List<Point> path, float speed)
    {
        if (_effectHandler != null) {
            Piece target = _tsprite.getPiece();
            path.add(new Point(target.x, target.y));
        }
        return super.createPath(board, path, speed);
    }

    @Override // documentation inherited
    protected Path createPath (
            Vector3f[] coords, float[] durations, String action)
    {
        if (_effectHandler != null) {
            MoveShootHandler handler = (MoveShootHandler)_effectHandler;
            durations[durations.length - 1] = 0f;
            return new BuffaloRiderPath(
                    this, coords, durations, _moveType, action, handler);
        }
        return super.createPath(coords, durations, action);
    }
}
