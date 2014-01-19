//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Handles displaying the proper damage value with a damage icon during 
 * a collision.
 */
public class CollisionHandler extends EffectHandler
{
    @Override // documentation inherited
    public void pieceMoved (Piece piece)
    {
        MobileSprite ms = null;
        if (piece.canBePushed()) { // don't push the pusher
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
}
