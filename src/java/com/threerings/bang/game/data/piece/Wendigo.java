//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Rectangle;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.WendigoSprite;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.WendigoEffect;

/**
 * Handles the behavior of the wendigo in the wendigo attack scenario.
 */
public class Wendigo extends Piece
    implements PieceCodes
{
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new WendigoSprite();
    }

    @Override // documentation inherited
    public int getTicksPerMove ()
    {
        return 2;
    }

    /**
     * Returns a WendigoEffect for attacking the board.
     */
    public WendigoEffect attack (BangObject bangobj)
    {
        WendigoEffect effect = new WendigoEffect();
        effect.pieceId = pieceId;
        Rectangle playarea = bangobj.board.getPlayableArea();
        switch (orientation) {
          case NORTH:
            effect.nx = x;
            effect.ny = playarea.y - 2;
            break;
          case SOUTH:
            effect.nx = x;
            effect.ny = playarea.y + playarea.height;
            break;
          case EAST:
            effect.ny = y;
            effect.nx = playarea.x + playarea.width;
            break;
          case WEST:
            effect.ny = y;
            effect.nx = playarea.x - 2;
            break;
        }
        return effect;
    }
}
