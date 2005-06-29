//
// $Id$

package com.threerings.bang.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.piece.Piece;

/**
 * Represents the effect of a piece activating a bonus.
 */
public abstract class Effect extends SimpleStreamableObject
{
    /** Provides a mechanism for observing the individual effects that
     * take place when applying an effect to the board and pieces. */
    public static interface Observer
    {
        /**
         * Indicates that the specified piece was added to the board.
         */
        public void pieceAdded (Piece piece);

        /**
         * Indicates that the specified piece was effected with the named
         * effect.
         */
        public void pieceAffected (Piece piece, String effect);

        /**
         * Indicates that the specified piece was removed from the board.
         */
        public void pieceRemoved (Piece piece);
    }

    /** Initializes this effect (called only on the server) with the piece
     * that activated the bonus. */
    public void init (Piece piece)
    {
    }

    /**
     * Prepares this effect for application. This is executed on the
     * server before the effect is applied on the server and then
     * distributed to the client for application there. The effect should
     * determine which pieces it will impact as well as decide where it
     * will be placing new pieces (and update the board shadow to reflect
     * those piece additions, though it should not actually add the pieces
     * until it is applied).
     *
     * @param dammap a mapping that should be used to record damage done
     * to a particular player's units (player index -> accumulated
     * damage).
     */
    public abstract void prepare (BangObject bangobj, IntIntMap dammap);

    /**
     * Applies this effect to the board and pieces. Any modifications to
     * pieces or the board should be made directly as this is executed on
     * both the client and server.
     *
     * @param observer an observer to inform of piece additions, updates
     * and removals (for display purposes on the client). This may be
     * null.
     */
    public abstract void apply (BangObject bangobj, Observer observer);

    /** A helper function for reporting a piece addition. */
    protected static void reportAddition (Observer obs, Piece piece)
    {
        if (obs != null) {
            obs.pieceAdded(piece);
        }
    }

    /** A helper function for reporting a piece affecting. */
    protected static void reportEffect (
        Observer obs, Piece piece, String effect)
    {
        if (obs != null) {
            obs.pieceAffected(piece, effect);
        }
    }

    /** A helper function for reporting a piece addition. */
    protected static void reportRemoval (Observer obs, Piece piece)
    {
        if (obs != null) {
            obs.pieceRemoved(piece);
        }
    }
}
