//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.UnitSprite;

import com.threerings.bang.game.data.effect.MoveShootEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

import com.threerings.bang.game.data.piece.Piece;

import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import static com.threerings.bang.Log.log;

/**
 * Animates a "shot" which occurs as part of a movement.
 */
public class MoveShootHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        _moveShoot = (MoveShootEffect)_effect;
        _shot = _moveShoot.shotEffect;
        _shooter = _bangobj.pieces.get(_moveShoot.pieceId);
        if (_shooter == null) {
            log.warning("Missing shooter?", "moveShoot", _moveShoot);
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
            if (ssprite instanceof UnitSprite) {
                _shotPender = notePender();
                ((UnitSprite)ssprite).setShootHandler(this);
            }
        }

        _applying = true;
        apply(_moveShoot);
        _applying = false;

        // now determine whether or not anything remained pending
        return !isCompleted();
    }

    /**
     * Called when the sprite is at the shooting portion of their path.
     */
    public void fireShot ()
    {
        playSounds(_bangSounds, true);
        
        _applying = true;
        apply(_shot);
        _applying = false;
        maybeComplete(_shotPender);
    }

    @Override // documentation inherited
    public void pieceMoved (Piece piece)
    {
        MobileSprite ms = null;
        if (piece == _target && _shot.pushx != -1 && _shot.pushAnim) {
            PieceSprite sprite = _view.getPieceSprite(piece);
            if (sprite != null && sprite instanceof MobileSprite) {
                ms = (MobileSprite)sprite;
                ms.setMoveAction(MobileSprite.MOVE_PUSH);
            }
        }

        super.pieceMoved(piece);

        if (piece == _target && ms != null) {
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

    protected MoveShootEffect _moveShoot;
    protected int _shotPender;
    protected ShotEffect _shot;
    protected Sound[] _bangSounds;

    protected Piece _shooter, _target;
}
