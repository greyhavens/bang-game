//
// $Id$

package com.threerings.bang.client;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

import com.threerings.jme.sprite.BallisticPath;
import com.threerings.jme.sprite.OrientingBallisticPath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.client.sprite.ShotSprite;
import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.effect.ShotEffect;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Waits for all sprites involved in a shot to stop moving and then
 * animates the fired shot.
 */
public class BallisticShotHandler
    implements PathObserver
{
    public BallisticShotHandler (BangContext ctx, BangObject bangobj,
                                 BangBoardView view, ShotEffect shot)
    {
        _ctx = ctx;
        _bangobj = bangobj;
        _view = view;
        _shot = shot;

        _shooter = (Piece)_bangobj.pieces.get(shot.shooterId);
        if (_shooter == null) {
            log.warning("Missing shooter? [shot=" + shot + "].");
            // abandon ship, we're screwed
            return;
        }
        _target = (Piece)_bangobj.pieces.get(shot.targetId);
        if (_target == null) {
            log.warning("Missing target? [shot=" + shot + "].");
            // abandon ship, we're screwed
            return;
        }

        // figure out which sprites we need to wait for
        considerPiece(_shooter);
        considerPiece(_target);

        // if no one was managed, it's a shot fired from an invisible
        // piece at invisible pieces, ignore it
        if (_managed == 0) {
            log.info("Tree feel in the woods, no one was around.");

        } else if (_sprites == 0) {
            // if we're not waiting for any sprites to finish moving,
            // fire the shot immediately
            fireShot();
        }
    }

    // documentation inherited from interface PathObserver
    public void pathCompleted (Sprite sprite, Path path)
    {
        sprite.removeObserver(this);
        if (sprite == _ssprite) {
            _view.applyShot(_shot);
            _view.removeSprite(sprite);
        } else if (--_sprites == 0) {
            fireShot();
        }
    }

    // documentation inherited from interface PathObserver
    public void pathCancelled (Sprite sprite, Path path)
    {
        sprite.removeObserver(this);
        if (sprite == _ssprite) {
            _view.applyShot(_shot);
            _view.removeSprite(sprite);
        } else if (--_sprites == 0) {
            fireShot();
        }
    }

    protected void considerPiece (Piece piece)
    {
        PieceSprite sprite = null;
        if (piece != null) {
            sprite = _view.getPieceSprite(piece);
        }
        if (sprite == null) {
            return;
        }
        if (_view.isManaged(sprite)) {
            _managed++;
            if (sprite.isMoving()) {
                sprite.addObserver(this);
                _sprites++;
            }
        }
    }

    protected void fireShot ()
    {
        Vector3f start = new Vector3f(_shooter.x * TILE_SIZE + TILE_SIZE/2,
                                      _shooter.y * TILE_SIZE + TILE_SIZE/2,
                                      TILE_SIZE/2);
        Vector3f end = new Vector3f(_target.x * TILE_SIZE + TILE_SIZE/2,
                                    _target.y * TILE_SIZE + TILE_SIZE/2,
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

    protected BangContext _ctx;
    protected BangBoardView _view;
    protected BangObject _bangobj;
    protected ShotEffect _shot;

    protected ShotSprite _ssprite;
    protected Piece _shooter, _target;
    protected int _sprites, _managed;

    protected static final float GRAVITY = 10*BallisticPath.G;
    protected static final Vector3f GRAVVEC = new Vector3f(0, 0, GRAVITY);
}
