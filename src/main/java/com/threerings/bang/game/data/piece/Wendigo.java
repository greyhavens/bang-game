//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.WendigoSprite;

import com.threerings.bang.game.data.BangBoard;

/**
 * Handles the behavior of the wendigo in the wendigo attack scenario.
 */
public class Wendigo extends Piece
    implements PieceCodes
{
    public Wendigo ()
    {
    }

    public Wendigo (boolean claw)
    {
        _claw = claw;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new WendigoSprite(_claw);
    }

    @Override // documentation inherited
    public int getTicksPerMove ()
    {
        return 4;
    }

    @Override // documentation inherited
    public int computeElevation (
            BangBoard board, int tx, int ty, boolean moving)
    {
        int z = board.getHeightfieldElevation(tx, ty);
        if (orientation == EAST || orientation == WEST) {
            ty++;
        } else {
            tx++;
        }
        return (z + board.getHeightfieldElevation(tx, ty)) / 2;
    }

    protected boolean _claw = false;
}
