//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

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
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.util.SoundUtil;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Waits for all sprites involved in a shot to stop moving and then
 * animates the fired shot.
 */
public class BallisticShotHandler extends ShotHandler
    implements PathObserver
{
    @Override // documentation inherited
    protected void prepareSounds (SoundGroup sounds)
    {
        super.prepareSounds(sounds);

        // if the shooter has a "launch" sound, grab that
        if (_shooter instanceof Unit) {
            Unit sunit = (Unit)_shooter;
            String lpath = "rsrc/units/" + sunit.getType() + "/launch.wav";
            if (SoundUtil.haveSound(lpath)) {
                _launchSound = sounds.getSound(lpath);
            }
        }

        // if there will be a deflection, load the deflect shot sound
        if (_shot.deflectorIds != null) {
            _deflectSound = sounds.getSound(
                "rsrc/sounds/effects/deflect_shot.wav");
        }
    }

    @Override // documentation inherited
    protected void fireShot (int sx, int sy, int tx, int ty)
    {
        float escale = _bangobj.board.getElevationScale(TILE_SIZE);
        Vector3f start = new Vector3f(
            sx * TILE_SIZE + TILE_SIZE/2, sy * TILE_SIZE + TILE_SIZE/2,
            _bangobj.board.getElevation(sx, sy) * escale + TILE_SIZE/2);
        Vector3f end = new Vector3f(
            tx * TILE_SIZE + TILE_SIZE/2, ty * TILE_SIZE + TILE_SIZE/2,
            _bangobj.board.getElevation(tx, ty) * escale + TILE_SIZE/2);
        _ssprite = new ShotSprite(
            _ctx, "units", "frontier_town/artillery/shell",
            _view.getUnitSprite(_shooter).getColorizations());
        Vector3f velvec = end.subtract(start);
        float edelta = velvec.z;
        velvec.z = 0f;
        float distance = velvec.length();

        float angle = 3*FastMath.PI/8;
        float duration = FastMath.sqrt(
            2 * (edelta - distance * FastMath.tan(angle)) / GRAVITY),
            velocity = distance / (duration * FastMath.cos(angle));

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

//             log.info("Distance " + distance + " angle " + angle +
//                      " velocity " + velocity + " duration " + duration +
//                      " axis " + axis +
//                      " velvec " + velvec + " (" + velvec.length() + ")");

        _penderId = notePender();
        _ssprite.setLocalTranslation(start);
        _ssprite.addObserver(this);
        _view.addSprite(_ssprite);
        
        // for sprites deflecting the shot to another coordinate, run the
        // blocking animation just before the end of the path
        final MobileSprite dsprite = getDeflectorSprite();
        final float btime = duration - (dsprite == null ?
            0f : getActionDuration(dsprite, "blocking") * 0.5f);
        _ssprite.move(new OrientingBallisticPath(
                          _ssprite, new Vector3f(1, 0, 0), start, velvec,
                          GRAVVEC, duration) {
            public void update (float time) {
                super.update(time);
                if (dsprite != null && !_blocking && _accum >= btime) {
                    _deflectSound.play(false);
                    queueAction(dsprite, "blocking");
                    _blocking = true;
                }
            }
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
        sprite.removeObserver(this);
        _view.removeSprite(sprite);

        // make a note of our pender id because fireNextSegment() may result in
        // a new call to fireShot() which will overwrite _penderId, but we want
        // to wait until we're all done firing the next shot before allowing
        // this action to finish
        int penderId = _penderId;
        if (!fireNextSegment()) {
            if (_target != null && _bangSound != null) {
                _bangSound.play(false); // bang!
            }

            // apply the effect and complete our handling if that did not
            // result in anything that needs waiting for
            _effect.apply(_bangobj, this);
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
        _effect.apply(_bangobj, this);
        maybeComplete(_penderId);
    }

    /**
     * Returns the sprite that is deflecting the shot at this stage, or
     * <code>null</code> for none.
     */
    protected MobileSprite getDeflectorSprite ()
    {
        if (_shot.deflectorIds != null && _sidx < _shot.deflectorIds.length) {
            int pieceId = _shot.deflectorIds[_sidx];
            Piece piece = _bangobj.pieces.get(pieceId);
            if (piece != null) {
                return _view.getUnitSprite(piece);
            } else {
                log.warning("Missing shot deflector [pieceId=" + pieceId +
                    "].");
            }
        }
        return null;
    }
    
    /**
     * Returns the duration of the sprite's action, or zero if the sprite has
     * no such action.
     */
    protected float getActionDuration (MobileSprite sprite, String action)
    {
        Model.Animation anim = sprite.getAction(action);
        return (anim == null) ? 0f : anim.getDuration();
    }
    
    protected int _penderId;
    protected ShotSprite _ssprite;
    protected Sound _launchSound, _deflectSound;

    protected static final float GRAVITY = 10*BallisticPath.G;
    protected static final Vector3f GRAVVEC = new Vector3f(0, 0, GRAVITY);
}
