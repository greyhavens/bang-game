//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Spatial;

import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.BallisticPath;
import com.threerings.jme.sprite.OrientingBallisticPath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;

import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.ShotSprite;
import com.threerings.bang.game.client.sprite.FireworksSprite;
import com.threerings.bang.game.client.effect.EffectViz;
import com.threerings.bang.game.client.effect.ExplosionViz;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.RocketEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.util.SoundUtil;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Waits for all sprites involved in a shot to stop moving and then
 * animates the fired shot.
 */
public class RocketHandler extends EffectHandler
    implements PathObserver
{
    /** The acceleration due to gravity. */
    protected static final float GRAVITY = 20 * BallisticPath.G;

    /** The gravity vector used for path acceleration. */
    public static final Vector3f GRAVITY_VECTOR = new Vector3f(0, 0, GRAVITY);

    /** Contains generated ballistic path parameters. */
    public static class PathParams
    {
        /** The initial velocity. */
        public Vector3f velocity;

        /** The flight duration. */
        public float duration;

        public PathParams (Vector3f velocity, float duration)
        {
            this.velocity = velocity;
            this.duration = duration;
        }
    }

    @Override // documentation inherited
    public boolean execute ()
    {
        _shot = (RocketEffect)_effect;
        if (_shot.shooter == null) {
            log.warning("Missing shooter? [shot=" + _shot + "].");
            // abandon ship, we're screwed
            return false;
        }
        if (_shot.targetId != -1) {
            _target = _bangobj.pieces.get(_shot.targetId);
            if (_target == null) {
                log.warning("Missing target? [shot=" + _shot + "].");
            }
        }

        fireShot();

        // now determine whether or not anything remained pending
        return !isCompleted();
    }

    /**
     * Computes and returns ballistic path parameters for a path that starts
     * and ends at the specified points.
     */
    public static PathParams computePathParams (Vector3f start, Vector3f end)
    {
        Vector3f velvec = end.subtract(start);
        float edelta = velvec.z;
        velvec.z = 0f;
        float distance = velvec.length();

        float minAngle = (distance < FastMath.FLT_EPSILON) ?
            FastMath.HALF_PI : FastMath.atan(edelta / distance);
        float angle = Math.max(3*FastMath.PI/8, FastMath.PI/4 + minAngle/2);
        float duration, velocity;
        if (distance < FastMath.FLT_EPSILON) {
            if (edelta < 0f) { // just let it drop
                duration = FastMath.sqrt(2f * edelta / GRAVITY);
                velvec.set(0f, 0f, 0f);
            } else { // velocity reaches zero at target height
                duration = FastMath.sqrt(-2f * edelta / GRAVITY);
                velocity = -GRAVITY * duration;
                velvec.set(0f, 0f, velocity);
            }
            return new PathParams(velvec, duration);

        } else {
            duration = FastMath.sqrt(
                2 * (edelta - distance * FastMath.tan(angle)) / GRAVITY);
            velocity = distance / (duration * FastMath.cos(angle));
        }

        // normalize the velocity vector and scale it to the velocity
        velvec.normalizeLocal();
        velvec.multLocal(velocity);

        // rotate the velocity vector up by the computed angle (around
        // the axis made by crossing the velocity vector with the up
        // vector)
        Vector3f axis = velvec.cross(UP);
        axis.normalizeLocal();
        Quaternion rot = new Quaternion();
        rot.fromAngleAxis(angle, axis);
        rot.multLocal(velvec);

        return new PathParams(velvec, duration);
    }

    /**
     * Determines whether our shot has followed all the segments it needs to,
     * in which case false is returned. Otherwise the next shot segment is
     * started and true is returned.
     */
    protected boolean fireNextSegment ()
    {
        if (_sidx == _shot.xcoords.length-1) {
            return false;
        } else {
            _sidx++;
            fireShot();
            return true;
        }
    }

    protected void fireShot ()
    {
        if (_sidx == 0) {
            // pre-apply the shot effect which may update the shooter
            //_shot.preapply(_bangobj, this);

            // animate the shooter if this is not collateral damage (in which
            // case the main shot will trigger their shooting animation)
            /*if (_shot.type != ShotEffect.COLLATERAL_DAMAGE) {
                PieceSprite ssprite = _view.getPieceSprite(_shot.shooter);
                // on the first shot, we animate the shooter
                if (ssprite instanceof MobileSprite) {
                    animateShooter((MobileSprite)ssprite);
                }
            }*/

            // now fire the shot animation
            fireShot(_shot.shooter.x, _shot.shooter.y,
                     _shot.xcoords[_sidx], _shot.ycoords[_sidx]);

        } else {
            fireShot(_shot.xcoords[_sidx-1], _shot.ycoords[_sidx-1],
                     _shot.xcoords[_sidx], _shot.ycoords[_sidx]);
        }
    }

    protected void fireShot (int sx, int sy, int tx, int ty)
    {
        // if the shooter sprite has a node configured as a ballistic
        // shot source, use its translation; otherwise, just use a
        // point one half tile above the ground
        Vector3f start = null;
        FireworksSprite usprite = (FireworksSprite)_view.getTargetableSprite(_shot.shooter);
        if (_sidx == 0 && usprite != null) {
            Spatial src = usprite.getRocketSource();
            if (src != null) {
                start = new Vector3f(src.getWorldTranslation());
            }
        }
        float escale = _bangobj.board.getElevationScale(TILE_SIZE);
        if (start == null) {
            start = new Vector3f(
                sx * TILE_SIZE + TILE_SIZE/2, sy * TILE_SIZE + TILE_SIZE/2,
                _bangobj.board.getElevation(sx, sy) * escale + TILE_SIZE/2);
        }
        Vector3f end = new Vector3f(
            tx * TILE_SIZE + TILE_SIZE/2, ty * TILE_SIZE + TILE_SIZE/2,
            _bangobj.board.getElevation(tx, ty) * escale + TILE_SIZE/2);
        PathParams pparams;
        Vector3f gravity;
        RocketEffect bshot = (RocketEffect)_shot;
        if (bshot.getTrajectory() == RocketEffect.Trajectory.FLAT) {
            Vector3f velocity = end.subtract(start);
            float length = velocity.length();
            pparams = new PathParams(
                velocity.normalizeLocal().mult(FLAT_TRAJECTORY_SPEED),
                length / FLAT_TRAJECTORY_SPEED);
            gravity = Vector3f.ZERO;
        } else { // RocketEffect.Trajectory.HIGH_ARC
            pparams = computePathParams(start, end);
            gravity = GRAVITY_VECTOR;
        }

        _ssprite = new ShotSprite(_ctx, bshot.getShotType(),
            (usprite == null) ? null : usprite.getColorizations());

        _penderId = notePender();
        _ssprite.setLocalTranslation(start);
        _ssprite.addObserver(this);
        _view.addSprite(_ssprite);

        // for sprites deflecting the shot to another coordinate, run the
        // blocking animation just before the end of the path
        //final MobileSprite dsprite = getDeflectorSprite();
        //final float btime = pparams.duration - (dsprite == null ?
        //    0f : getActionDuration(dsprite, "blocking") * 0.5f);
        final float delay = (_sidx == 0 && usprite != null) ?
            usprite.getRocketDelay() : 0f;
        _ssprite.setCullMode(Spatial.CULL_ALWAYS);
        _ssprite.move(new OrientingBallisticPath(_ssprite,
            new Vector3f(1, 0, 0), start, pparams.velocity, gravity,
            pparams.duration) {
            public void update (float time) {
                if ((_daccum += time) < delay) {
                    return;
                }
                _ssprite.setCullMode(Spatial.CULL_DYNAMIC);
                super.update(time);
                /*if (dsprite != null && !_blocking && _accum >= btime) {
                    _deflectSound.play(false);
                    queueAction(dsprite, "blocking");
                    _blocking = true;
                }*/
            }
            float _daccum;
            boolean _blocking;
        });

        if (_sidx == 0) {
            // play the launch sound if we have one
            if (_launchSound != null) {
                _launchSound.play(false);
            }
        }
    }

    // documentation inherited from interface PathObserver
    public void pathCompleted (Sprite sprite, Path path)
    {
        Vector3f spriteTranslation = sprite.getLocalTranslation();
        sprite.removeObserver(this);
        _view.removeSprite(sprite);

        // make a note of our pender id because fireNextSegment() may result in
        // a new call to fireShot() which will overwrite _penderId, but we want
        // to wait until we're all done firing the next shot before allowing
        // this action to finish
        int penderId = _penderId;
        if (!fireNextSegment()) {
            //if (_target != null) {
                //playSounds(_bangSounds, true);
            //}

            EffectViz viz = new ExplosionViz();
            viz.init(_ctx, _view, spriteTranslation, new EffectViz.Observer() {
                public void effectDisplayed () {
                    //sprite.updated(_target, _tick);
                    maybeComplete(_penderId);
                }
            });
            viz.display();

            // apply the effect and complete our handling if that did not
            // result in anything that needs waiting for
            apply(_effect);
        }
        maybeComplete(penderId);
    }

    // documentation inherited from interface PathObserver
    public void pathCancelled (Sprite sprite, Path path)
    {
        sprite.removeObserver(this);
        _view.removeSprite(sprite);

        // apply the effect and complete our handling if that did not
        // result in anything that needs waiting for
        apply(_effect);
        maybeComplete(_penderId);
    }

    /**
     * Returns the sprite that is deflecting the shot at this stage, or
     * <code>null</code> for none.
     */
    /*protected MobileSprite getDeflectorSprite ()
    {
        if (_shot.deflectorIds != null && _sidx < _shot.deflectorIds.length) {
            //int pieceId = _shot.deflectorIds[_sidx];
            Piece piece = _bangobj.pieces.get(pieceId);
            if (piece != null) {
                return _view.getUnitSprite(piece);
            } else {
                log.warning("Missing shot deflector [pieceId=" + pieceId +
                    "].");
            }
        }
        return null;
    }*/

    /**
     * Returns the duration of the sprite's action, or zero if the sprite has
     * no such action.
     */
    protected float getActionDuration (MobileSprite sprite, String action)
    {
        Model.Animation anim = sprite.getAction(action);
        return (anim == null) ? 0f : anim.getDuration();
    }

    protected RocketEffect _shot;
    protected Piece _target;
    protected int _sidx;
    protected int _penderId;
    protected ShotSprite _ssprite;
    protected Sound _launchSound, _deflectSound;

    /** The speed (u/s) at which to fire projectiles with flat trajectories. */
    protected static final float FLAT_TRAJECTORY_SPEED = 50f;
}
