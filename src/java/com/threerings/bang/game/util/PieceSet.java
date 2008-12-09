//
// $Id$

package com.threerings.bang.game.util;

import java.util.HashMap;

import com.threerings.bang.game.data.piece.Piece;

/**
 * Contains a set of pieces.
 */
public class PieceSet extends HashMap<Comparable<?>,Piece>
{
    public void add (Piece piece)
    {
        put(piece.getKey(), piece);
    }
}
