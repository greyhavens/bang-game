//
// $Id$

package com.samskivert.bang.data.effect;

import java.util.ArrayList;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.util.PieceSet;

/**
 * Represents the effect of a piece activating a bonus.
 */
public abstract class Effect extends SimpleStreamableObject
{
    /**
     * Applies this effect to the board and pieces. Any modifications to
     * pieces or the board should be made directly. Newly added pieces
     * should be added to the supplied array list and pieces to be removed
     * should be added to the removals set.
     */
    public abstract void apply (BangObject bangobj, ArrayList<Piece> additions,
                                PieceSet removals);
}
