//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;
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
public abstract class ShotHandler
    implements PathObserver
{
    public void init (BangContext ctx, BangObject bangobj, BangBoardView view,
                      SoundGroup sounds, ShotEffect shot)
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
        if (shot.targetId != -1) {
            _target = (Piece)_bangobj.pieces.get(shot.targetId);
            if (_target == null) {
                log.warning("Missing target? [shot=" + shot + "].");
            }
        }

        // prepare our sounds
        prepareSounds(sounds);

        // figure out which sprites we need to wait for
        PieceSprite ssprite = considerPiece(_shooter);
        if (_target != null) {
            PieceSprite tsprite = considerPiece(_target);
            // let the shooting sprite know that it will be shooting the
            // specified target at the end of its path
            if (ssprite instanceof MobileSprite) {
                ((MobileSprite)ssprite).willShoot(_target, tsprite);
            }
        }

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
        if (--_sprites == 0) {
            fireShot();
        }
    }

    // documentation inherited from interface PathObserver
    public void pathCancelled (Sprite sprite, Path path)
    {
        sprite.removeObserver(this);
        if (--_sprites == 0) {
            fireShot();
        }
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

    protected PieceSprite considerPiece (Piece piece)
    {
        PieceSprite sprite = null;
        if (piece != null) {
            sprite = _view.getPieceSprite(piece);
        }
        if (sprite == null) {
            return null;
        }
        if (!_view.isManaged(sprite)) {
            return null;
        }

        _managed++;
        if (sprite.isMoving()) {
            sprite.addObserver(this);
            _sprites++;
        }
        return sprite;
    }

    protected void fireShot ()
    {
        if (_sidx == 0) {
            fireShot(_shooter.x, _shooter.y,
                     _shot.xcoords[_sidx], _shot.ycoords[_sidx]);
            // don't animate the shooter for collateral damage shots, the
            // main shot will trigger an animation
            if (_shot.type != ShotEffect.COLLATERAL_DAMAGE) {
                // on the first shot, we animate the shooter
                PieceSprite ssprite = _view.getPieceSprite(_shooter);
                if (ssprite instanceof MobileSprite) {
                    ((MobileSprite)ssprite).queueAction(
                        ShotEffect.SHOT_ACTIONS[_shot.type]);
                }
            }

        } else {
            fireShot(_shot.xcoords[_sidx-1], _shot.ycoords[_sidx-1],
                     _shot.xcoords[_sidx], _shot.ycoords[_sidx]);
        }
    }

    /**
     * Determines whether our shot has followed all the segments it needs
     * to, in which case false is returned. Otherwise the next shot
     * segment is started and true is returned.
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

    protected BangContext _ctx;
    protected BangBoardView _view;
    protected BangObject _bangobj;
    protected ShotEffect _shot;
    protected Sound _bangSound;

    protected Piece _shooter, _target;
    protected int _sprites, _managed, _sidx;
}
