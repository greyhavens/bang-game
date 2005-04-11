//
// $Id$

package com.samskivert.bang.util;

import java.util.HashMap;

import com.samskivert.bang.data.piece.Piece;

/**
 * Contains a set of pieces.
 */
public class PieceSet extends HashMap<Comparable,Piece>
{
    public void add (Piece piece)
    {
        put(piece.getKey(), piece);
    }
}
