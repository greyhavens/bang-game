//
// $Id$

package com.samskivert.bang.data.piece;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.client.sprite.TankSprite;

import static com.samskivert.bang.Log.log;

/**
 * Handles the state and behavior of the tank piece.
 */
public class Tank extends Piece
    implements PlayerPiece
{
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TankSprite();
    }
}
