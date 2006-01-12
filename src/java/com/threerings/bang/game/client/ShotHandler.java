//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Waits for all sprites involved in a shot to stop moving and then
 * animates the fired shot.
 */
public abstract class ShotHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        _shot = (ShotEffect)_effect;
        _shooter = (Piece)_bangobj.pieces.get(_shot.shooterId);
        if (_shooter == null) {
            log.warning("Missing shooter? [shot=" + _shot + "].");
            // abandon ship, we're screwed
            return false;
        }
        if (_shot.targetId != -1) {
            _target = (Piece)_bangobj.pieces.get(_shot.targetId);
            if (_target == null) {
                log.warning("Missing target? [shot=" + _shot + "].");
            }
        }

        // prepare our sounds
        prepareSounds(_sounds);

        // if we have a target, let the shooter's sprite know that it will be
        // shooting the specified target
        if (_target != null) {
            PieceSprite ssprite = _view.getPieceSprite(_shooter);
            if (ssprite instanceof MobileSprite) {
                ((MobileSprite)ssprite).willShoot(
                    _target, _view.getPieceSprite(_target));
            }
        }

        // note that we're not going to be "completed" until we're done doing
        // all the firing business so that no one fires off an
        // instant-completion effect and then thinks we're ready to call it
        // quits
        _shooting = true;
        fireShot();
        _shooting = false;

        return !isCompleted();
    }

    @Override // documentation inherited
    protected boolean isCompleted ()
    {
        return !_shooting && super.isCompleted();
    }

    /**
     * Prepares the sounds we'll need during the animation of this shot.
     */
    protected void prepareSounds (SoundGroup sounds)
    {
        // load up the sound that will go with our shootin'
        if (_shooter instanceof Unit) {
            String type = ((Unit)_shooter).getType();
            // no sound for collateral damage shot; the main shot will
            // produce a sound
            if (_shot.type != ShotEffect.COLLATERAL_DAMAGE) {
                _bangSound = sounds.getSound(
                    "rsrc/units/" + type + "/" +
                    ShotEffect.SHOT_ACTIONS[_shot.type] + ".wav");
            }
        }
    }

    protected void fireShot ()
    {
        if (_sidx == 0) {
            fireShot(_shooter.x, _shooter.y,
                     _shot.xcoords[_sidx], _shot.ycoords[_sidx]);
            // don't animate the shooter for collateral damage shots, the main
            // shot will trigger an animation
            if (_shot.type != ShotEffect.COLLATERAL_DAMAGE) {
                // on the first shot, we animate the shooter
                PieceSprite ssprite = _view.getPieceSprite(_shooter);
                if (ssprite instanceof MobileSprite) {
                    queueAction((MobileSprite)ssprite,
                                ShotEffect.SHOT_ACTIONS[_shot.type]);
                }
            }

        } else {
            fireShot(_shot.xcoords[_sidx-1], _shot.ycoords[_sidx-1],
                     _shot.xcoords[_sidx], _shot.ycoords[_sidx]);
        }
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

    protected abstract void fireShot (int sx, int sy, int tx, int ty);

    protected ShotEffect _shot;
    protected Sound _bangSound;

    protected boolean _shooting;
    protected Piece _shooter, _target;
    protected int _sidx;
}
