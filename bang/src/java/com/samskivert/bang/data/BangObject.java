//
// $Id$

package com.samskivert.bang.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.parlor.game.data.GameObject;

import com.samskivert.bang.data.piece.Piece;

/**
 * Contains all distributed information for the game.
 */
public class BangObject extends GameObject
{
    /** The invocation service via which the client communicates with the
     * server. */
    public BangMarshaller service;

    /** The curent board tick count. */
    public short tick;

    /** Contains the representation of the game board. */
    public BangBoard board;

    /** Contains information on all pieces on the board. */
    public DSet pieces;

    /** Returns the {@link #pieces} set as an array to allow for
     * simultaneous iteration and removal. */
    public Piece[] getPieceArray ()
    {
        return (Piece[])pieces.toArray(new Piece[pieces.size()]);
    }
}
