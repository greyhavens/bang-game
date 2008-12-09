//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;

import com.threerings.jme.sprite.BallisticPath;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.effect.ReboundEffect;
import com.threerings.bang.game.data.effect.SpringShotEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.*;

/**
 * Launches units up into the air and lands them at a new location.
 */
public class ReboundHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        if (_effect instanceof ShotEffect) {
            
            // if we have a target, let the shooter's sprite know that it will be
            // shooting the specified target
            ShotEffect shot = (ShotEffect)_effect;
            Piece target = _bangobj.pieces.get(shot.targetId);
            Piece shooter = _bangobj.pieces.get(shot.shooterId);
            if (target != null && shooter != null) {
                PieceSprite ssprite = _view.getPieceSprite(shooter);
                if (ssprite instanceof MobileSprite) {
                    ((MobileSprite)ssprite).willShoot(
                        target, _view.getPieceSprite(target));
                }
            }
            
            // pre-apply the shot effect which may update the shooter
            shot.preapply(_bangobj, this);
        }
        
        // now determine whether or not anything remained pending
        return super.execute();        
    }
    
    @Override // documentation inherited
    public void pieceMoved (final Piece piece)
    {
        // launch the piece into the air
        final PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            log.warning("Missing target sprite for rebound effect", "piece", piece);
            return;
        }
        Vector3f start = new Vector3f(sprite.getWorldTranslation());
        float duration = FastMath.sqrt(
            -2f * PIECE_DROP_HEIGHT / BallisticShotHandler.GRAVITY);
        final int penderId = notePender();
        sprite.move(new BallisticPath(sprite, start,
            new Vector3f(0f, 0f, 2f * PIECE_DROP_HEIGHT),
            BallisticShotHandler.GRAVITY_VECTOR, duration) {
            public void wasRemoved () {
                super.wasRemoved();
                updateAndDrop(piece, sprite);
                maybeComplete(penderId);
            }
        });
    }
    
    @Override // documentation inherited
    public void pieceDropped (Piece piece)
    {
		if (_effect instanceof ReboundEffect) {
        	((ReboundEffect)_effect).finishTrapPiece(_bangobj, this);
    	} else if (_effect instanceof SpringShotEffect) {
            ((SpringShotEffect)_effect).pieceDropped(_bangobj, this);
		} else {
			super.pieceDropped(piece);
		}
    }
    
    /**
     * Moves the piece to its new location and drops it onto the board.
     */
    protected void updateAndDrop (Piece piece, PieceSprite sprite)
    { 
        // update the sprite
        sprite.updated(piece, _tick);

        // let the board view know that this piece has moved
        _view.pieceDidMove(piece);
        
        // reposition the unit and drop it back onto the board
        sprite.setLocation(piece.x, piece.y,
            piece.computeElevation(_bangobj.board, piece.x, piece.y));
        sprite.setOrientation(piece.orientation);
        dropPiece(piece);
    }
}
