//
// $Id$

package com.samskivert.bang.data.generate;

import java.util.ArrayList;

import com.threerings.toybox.data.ToyBoxGameConfig;

import com.samskivert.bang.data.BangBoard;
import com.samskivert.bang.data.piece.Piece;

/**
 * Provides a framework for generating the pieces and goals that make up a
 * scenario (generally done after an environment is generated).
 */
public abstract class ScenarioGenerator
{
    /**
     * Instructs the generator to perform its generation, modifying the
     * supplied board and adding any created pieces to the supplied list.
     *
     * @param config the game configuration.
     */
    public abstract void generate (
        ToyBoxGameConfig config, BangBoard board, ArrayList<Piece> pieces);

    /**
     * Configures a piece with the proper piece id and owner id and then
     * adds it to the pieces array.
     */
    protected void configureAndAdd (
        ArrayList<Piece> pieces, int pidx, Piece piece)
    {
        piece.assignPieceId();
        piece.owner = pidx;
        pieces.add(piece);
    }

    /**
     * Attempts to place the specified piece at the specified location.
     * Returns false if such a placement is not possible due to the space
     * being occupied by another piece or being out of bounds.
     */
    protected boolean tryPlacement (
        BangBoard board, ArrayList<Piece> pieces, int tx, int ty, Piece piece)
    {
        if (tx < 0 || tx >= board.getWidth() ||
            ty < 0 || ty >= board.getHeight() || tileOccupied(pieces, tx, ty)) {
            return false;
        }

        piece.position(tx, ty);
        pieces.add(piece);
        return true;
    }

    /**
     * Returns true if the specified tile is occupied by another piece.
     */
    protected boolean tileOccupied (ArrayList<Piece> pieces, int tx, int ty)
    {
        for (Piece piece : pieces) {
            if (piece.intersects(tx, ty)) {
                return true;
            }
        }
        return false;
    }
}
