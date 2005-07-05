//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;
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
    public void init (BangContext ctx, BangObject bangobj,
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

    protected abstract void fireShot ();

    protected BangContext _ctx;
    protected BangBoardView _view;
    protected BangObject _bangobj;
    protected ShotEffect _shot;

    protected Piece _shooter, _target;
    protected int _sprites, _managed;
}
