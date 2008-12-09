//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

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
        _shooter = _bangobj.pieces.get(_shot.shooterId);
        if (_shooter == null) {
            log.warning("Missing shooter?", "shot", _shot);
            // abandon ship, we're screwed
            return false;
        }
        if (_shot.targetId != -1) {
            _target = _bangobj.pieces.get(_shot.targetId);
            if (_target == null) {
                log.warning("Missing target?", "shot", _shot);
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

        // don't allow the effect to complete until we're done applying
        // everything
        _applying = true;
        fireShot();
        _applying = false;

        // now determine whether or not anything remained pending
        return !isCompleted();
    }

    @Override // documentation inherited
    public void pieceMoved (Piece piece)
    {
        MobileSprite ms = null;
        if (_shot.pushx != -1 && _shot.pushAnim) {
            PieceSprite sprite = _view.getPieceSprite(piece);
            if (sprite != null && sprite instanceof MobileSprite) {
                ms = (MobileSprite)sprite;
                ms.setMoveAction(MobileSprite.MOVE_PUSH);
            }
        }

        super.pieceMoved(piece);

        if (ms != null) {
            ms.setMoveAction(MobileSprite.MOVE_NORMAL);
        }
    }

    /**
     * Prepares the sounds we'll need during the animation of this shot.
     */
    protected void prepareSounds (SoundGroup sounds)
    {
        // load up the sounds that will go with our shootin'
        _bangSounds = getShotSounds(_shooter, _shot);
    }

    protected void fireShot ()
    {
        if (_sidx == 0) {
            // pre-apply the shot effect which may update the shooter
            _shot.preapply(_bangobj, this);

            // animate the shooter if this is not collateral damage (in which
            // case the main shot will trigger their shooting animation)
            if (_shot.type != ShotEffect.COLLATERAL_DAMAGE) {
                PieceSprite ssprite = _view.getPieceSprite(_shooter);
                // on the first shot, we animate the shooter
                if (ssprite instanceof MobileSprite) {
                    animateShooter((MobileSprite)ssprite);
                }
            }

            // now fire the shot animation
            fireShot(_shooter.x, _shooter.y,
                     _shot.xcoords[_sidx], _shot.ycoords[_sidx]);

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

    /**
     * Called to add the shot animation for the shooter.
     */
    protected void animateShooter (MobileSprite sprite)
    {
        queueAction(sprite, ShotEffect.SHOT_ACTIONS[_shot.type]);
    }

    protected abstract void fireShot (int sx, int sy, int tx, int ty);

    protected ShotEffect _shot;
    protected Sound[] _bangSounds;

    protected Piece _shooter, _target;
    protected int _sidx;
}
