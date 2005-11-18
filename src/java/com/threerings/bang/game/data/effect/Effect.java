//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

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
         * Indicates that the specified piece was updated.
         */
        public void pieceUpdated (Piece opiece, Piece npiece);
        
        /**
         * Indicates that the specified piece was removed from the board.
         */
        public void pieceRemoved (Piece piece);
        
        /**
         * Indicates that the tick was delayed for the specified amount of time
         * in order to let an effect run its course.
         */
        public void tickDelayed (long extraTime);
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
     * both the client and server. <em>Note:</em> effects should always
     * compute and store the final result of their effects in {@link
     * #prepare} and then simply apply those results in {@link #apply}
     * rather than do any computation in {@link #apply} as we cannot rely
     * on the values in the piece during the apply to be the same as they
     * would be on the server when the effect is applied. The only truly
     * safe time to inspect the condition of the affected pieces is during
     * {@link #prepare}.
     *
     * @param observer an observer to inform of piece additions, updates
     * and removals (for display purposes on the client). This may be
     * null.
     */
    public abstract void apply (BangObject bangobj, Observer observer);

    /**
     * Creates the appropriate derivation of {@link EffectHandler} to render
     * this effect.  Default implemenation returns <code>null</code>,
     * indicating no special handler required.
     */
    public EffectHandler createHandler (BangObject bangobj)
    {
        return null;
    }
    
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

    /** A helper function for reporting a piece update. */
    protected static void reportUpdate (Observer obs, Piece opiece,
        Piece npiece)
    {
        if (obs != null) {
            obs.pieceUpdated(opiece, npiece);
        }
    }
    
    /** A helper function for reporting a piece addition. */
    protected static void reportRemoval (Observer obs, Piece piece)
    {
        if (obs != null) {
            obs.pieceRemoved(piece);
        }
    }
    
    /** A helper function for reporting a tick delay. */
    protected static void reportDelay (Observer obs, long extraTime)
    {
        if (obs != null) {
            obs.tickDelayed(extraTime);
        }
    }
}
