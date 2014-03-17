//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.Vector3f;

import com.threerings.jme.sprite.LineSegmentPath;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.game.client.sprite.ActiveSprite;
import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.ShotSprite;
import com.threerings.bang.game.data.effect.TeleportEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Teleporter;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles the {@link TeleportEffect} on the client.
 */
public class TeleportHandler extends EffectHandler
    implements ActiveSprite.ActionObserver
{
    @Override // documentation inherited
    public boolean execute ()
    {
        // find the piece sprite
        _teffect = (TeleportEffect)_effect;
        Piece piece = _bangobj.pieces.get(_teffect.pieceId);
        if (piece == null) {
            log.warning("Couldn't find target for teleporter effect", "pieceId", _teffect.pieceId);
            return false;
        }
        _sprite = (MobileSprite)_view.getPieceSprite(piece);
        if (_sprite == null) {
            log.warning("Missing target sprite for teleporter effect", "piece", piece);
            return false;
        }
        
        // determine the activation effect type
        Teleporter tporter = (Teleporter)_bangobj.pieces.get(_teffect.sourceId);
        String travelEffect;
        if (tporter != null) {
            _activateEffect = tporter.getEffect("activate");
            travelEffect = tporter.getEffect("travel");
        } else {
            log.warning("Couldn't find source for teleporter effect", "pieceId", _teffect.sourceId);
            _activateEffect = "indian_post/teleporter_1/activate";
            travelEffect = "indian_post/teleporter_1/travel";
        }
        
        // first, fire off the activation effect at the original position and
        // fade the unit out
        _sprite.displayParticles(_activateEffect, false);
        if (_teffect.damageEffect == null) {
            _sprite.queueAction(MobileSprite.TELEPORTED_OUT);
            _sprite.addObserver(this);
        } else {
            // apply the effect to kill the unit
            apply(_teffect);
            return true;
        }
        
        // send a fireball to the other teleporter
        int dx = _teffect.dest[0], dy = _teffect.dest[1],
            dh = piece.computeElevation(_bangobj.board, dx, dy);
        Vector3f start = new Vector3f(_sprite.getWorldTranslation()),
            end = new Vector3f(
                (dx + 0.5f) * TILE_SIZE,
                (dy + 0.5f) * TILE_SIZE,
                dh * _bangobj.board.getElevationScale(TILE_SIZE));
        Vector3f startup = _sprite.getWorldRotation().mult(Vector3f.UNIT_Z),
            endup = _view.getTerrainNode().getHeightfieldNormal(end.x, end.y);
        start.scaleAdd(0.25f * TILE_SIZE, startup, start);
        end.scaleAdd(0.25f * TILE_SIZE, endup, end);
        final ShotSprite ssprite = new ShotSprite(
            _ctx, "effects/" + travelEffect, null);
        _view.addSprite(ssprite);
         ssprite.move(new LineSegmentPath(ssprite, Vector3f.UNIT_Z,
            Vector3f.UNIT_X, new Vector3f[] { start, start, end, end },
            new float[] { 0.25f, 0.75f, 0.25f }) {
            public void wasRemoved () {
                super.wasRemoved();
                _view.removeSprite(ssprite);
            }
        });
        
        return true;
    }
    
    // documentation inherited from interface MobileSprite.ActionListener
    public void actionCompleted (Sprite sprite, String action)
    {
        if (action.equals(MobileSprite.TELEPORTED_OUT)) {
            // apply the effect to move the unit
            apply(_teffect);
        
            // reposition the unit
            Piece piece = _bangobj.pieces.get(_teffect.pieceId);
            _sprite.setLocation(piece.x, piece.y,
                piece.computeElevation(_bangobj.board, piece.x, piece.y));
            _sprite.setOrientation(piece.orientation);
        
            // fire off the activation effect at the new position and fade
            // the unit back in
            _sprite.displayParticles(_activateEffect, false);
            _sprite.queueAction(MobileSprite.TELEPORTED_IN);
            
        } else if (action.equals(MobileSprite.TELEPORTED_IN)) {
            _sprite.removeObserver(this);
            _view.actionCompleted(this);
        }
    }

    @Override // documentation inherited
    public void pieceMoved (Piece piece)
    {
        // update the sprite
        _sprite.updated(piece, _tick);

        // let the board view know that this piece has moved
        _view.pieceDidMove(piece);
    }
    
    /** The sprite being teleported. */
    protected MobileSprite _sprite;
    
    /** The activation effect type. */
    protected String _activateEffect;

    /** Reference to our TeleportEffect. */
    protected TeleportEffect _teffect;
}
