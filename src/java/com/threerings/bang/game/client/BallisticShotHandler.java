//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

import com.threerings.jme.sprite.BallisticPath;
import com.threerings.jme.sprite.OrientingBallisticPath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.game.client.sprite.ShotSprite;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Waits for all sprites involved in a shot to stop moving and then
 * animates the fired shot.
 */
public class BallisticShotHandler extends ShotHandler
{
    protected void fireShot (int sx, int sy, int tx, int ty)
    {
        Vector3f start = new Vector3f(
            sx * TILE_SIZE + TILE_SIZE/2, sy * TILE_SIZE + TILE_SIZE/2,
            TILE_SIZE/2);
        Vector3f end = new Vector3f(
            tx * TILE_SIZE + TILE_SIZE/2, ty * TILE_SIZE + TILE_SIZE/2,
            TILE_SIZE/2);
        _ssprite = new ShotSprite(_ctx);
        Vector3f velvec = end.subtract(start);
        float distance = velvec.length();

        float angle = -3*FastMath.PI/8;
        float velocity = FastMath.sqrt(
            distance * GRAVITY / FastMath.sin(2*angle));
        float duration = BallisticPath.computeFlightTime(
            distance, velocity, angle);

        // normalize the velocity vector and scale it to the velocity
        velvec.normalizeLocal();
        velvec.multLocal(velocity);

        // rotate the velocity vector up by the computed angle (around
        // the axis made by crossing the velocity vector with the up
        // vector)
        Vector3f axis = UP.cross(velvec);
        axis.normalizeLocal();
        Quaternion rot = new Quaternion();
        rot.fromAngleAxis(angle, axis);
        rot.multLocal(velvec);

//             log.info("Distance " + distance + " angle " + angle +
//                      " velocity " + velocity + " duration " + duration +
//                      " axis " + axis +
//                      " velvec " + velvec + " (" + velvec.length() + ")");

        _ssprite.setLocalTranslation(start);
        _ssprite.addObserver(this);
        _view.addSprite(_ssprite);
        _ssprite.move(new OrientingBallisticPath(
                          _ssprite, new Vector3f(1, 0, 0), start, velvec,
                          GRAVVEC, duration));
    }

    @Override // documentation inherited
    public void pathCompleted (Sprite sprite, Path path)
    {
        if (sprite == _ssprite) {
            sprite.removeObserver(this);
            _view.removeSprite(sprite);
            if (!fireNextSegment()) {
                _view.applyShot(_shot);
            }
        } else {
            super.pathCompleted(sprite, path);
        }
    }

    @Override // documentation inherited
    public void pathCancelled (Sprite sprite, Path path)
    {
        if (sprite == _ssprite) {
            sprite.removeObserver(this);
            _view.removeSprite(sprite);
            _view.applyShot(_shot);
        } else {
            super.pathCancelled(sprite, path);
        }
    }

    protected ShotSprite _ssprite;

    protected static final float GRAVITY = 10*BallisticPath.G;
    protected static final Vector3f GRAVVEC = new Vector3f(0, 0, GRAVITY);
}
