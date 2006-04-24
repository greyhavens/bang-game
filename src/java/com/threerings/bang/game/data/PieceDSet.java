//
// $Id$

package com.threerings.bang.game.data;

import java.util.Iterator;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.game.data.piece.Piece;

/**
 * Contains the collection of pieces on the board but exposes the ability
 * to add and remove pieces directly so that we can have effects that add
 * and remove pieces from the board.
 */
public class PieceDSet extends DSet<Piece>
{
    public PieceDSet (Iterator<Piece> pieces)
    {
        super(pieces);
    }

    public PieceDSet (Piece[] pieces)
    {
        super(pieces);
    }

    public PieceDSet ()
    {
    }

    /**
     * Adds a piece directly to this distributed set without creating the
     * necessary distributed events to make things work properly. This
     * exists so that effects (which are "applied") on the client and
     * server can add pieces to the game.
     */
    protected void addDirect (Piece piece)
    {
        add(piece);
    }

    /**
     * Removes a piece directly from this distributed set without creating
     * the necessary distributed events to make things work properly. This
     * exists so that effects (which are "applied") on the client and
     * server can remove pieces from the game.
     */
    protected boolean removeDirect (Piece piece)
    {
        return remove(piece);
    }
    
    /**
     * Updates a piece directly in this distributed set without creating the
     * necessary distributed events to make things work properly.  This exists
     * so that effects (which are "applied") on the client and server can
     * update pieces in the game.
     */
    protected Piece updateDirect (Piece piece)
    {
        return (Piece)update(piece);
    }
}
