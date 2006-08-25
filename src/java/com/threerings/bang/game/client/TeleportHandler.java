//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.game.client.sprite.ActiveSprite;
import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.data.effect.TeleportEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Teleporter;

import static com.threerings.bang.Log.*;

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
            log.warning("Couldn't find target for teleporter effect " +
                "[pieceId=" + _teffect.pieceId + "].");
            return false;
        }
        _sprite = (MobileSprite)_view.getPieceSprite(piece);
        if (_sprite == null) {
            log.warning("Missing target sprite for teleporter effect " +
                "[piece=" + piece + "].");
            return false;
        }
        
        // determine the activation effect type
        Teleporter tporter = (Teleporter)_bangobj.pieces.get(_teffect.sourceId);
        if (tporter != null) {
            _activateEffect = tporter.getActivateEffect();
        } else {
            log.warning("Couldn't find source for teleporter effect " +
                "[pieceId=" + _teffect.sourceId + "].");
            _activateEffect = "indian_post/teleporter_1/activate";
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
        }
        
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
        // nothing doing
    }
    
    /** The sprite being teleported. */
    protected MobileSprite _sprite;
    
    /** The activation effect type. */
    protected String _activateEffect;

    /** Reference to our TeleportEffect. */
    protected TeleportEffect _teffect;
}
