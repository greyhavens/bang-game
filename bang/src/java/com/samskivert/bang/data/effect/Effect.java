//
// $Id$

package com.samskivert.bang.data.effect;

import java.util.ArrayList;

import com.threerings.io.SimpleStreamableObject;

import com.samskivert.bang.data.BangBoard;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.util.PieceSet;

/**
 * Represents the effect of a piece activating a surprise.
 */
public abstract class Effect extends SimpleStreamableObject
{
    /**
     * Applies this effect to the board and pieces. Any modifications to
     * existing pieces should be made directly to the pieces in the array.
     * Newly added pieces should be added to the supplied array list and
     * pieces to be removed should be added to the removals set.
     */
    public abstract void apply (BangBoard board, Piece[] pieces,
                                ArrayList<Piece> additions, PieceSet removals);
}
