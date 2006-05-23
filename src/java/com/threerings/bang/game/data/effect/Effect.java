//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import java.util.Random;

import com.samskivert.util.IntIntMap;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.Stat;
import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Encapsulates any effect on a piece in the game. All state changes are
 * communicated through an ordered stream of effects.
 */
public abstract class Effect extends SimpleStreamableObject
{
    /** An effect to use when a piece's internal status is updated and it
     * should be refreshed, but no other visible change will take place. */
    public static final String UPDATED = "updated";

    /** Provides a mechanism for observing the individual effects that take
     * place when applying an effect to the board and pieces. */
    public static interface Observer
    {
        /**
         * Indicates that the specified piece was added to the board.
         */
        public void pieceAdded (Piece piece);

        /**
         * Indicates that the specified piece was affected with the named
         * effect. The piece's sprite should be updated to reflect the piece's
         * new internal state after an appropriate visualization has been
         * displayed.
         */
        public void pieceAffected (Piece piece, String effect);

        /**
         * Indicates that the specified piece was moved or reoriented.
         */
        public void pieceMoved (Piece piece);

        /**
         * Indicates that the piece in question was killed (killed pieces are
         * not automatically removed; if a piece was removed that will be
         * reported separately with a call to {@link #pieceRemoved}).
         */
        public void pieceKilled (Piece piece);

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

    /**
     * Handles a collision that moves and damages a unit.
     *
     * @param collider the index of the user causing the collision, or -1.
     * @param damage the amount of damage caused by the collision.
     */
    public static void collide (
        BangObject bangobj, Observer obs, int collider, int targetId,
        int damage, int x, int y, String effect)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing collision target [targetId=" + targetId +
                "].");
            return;
        }

        // move the target to its new coordinates
        if (target.x != x || target.y != y) {
            moveAndReport(bangobj, target, x, y, obs);
        }

        // damage the target if it's still alive
        if (target.isAlive()) {
            damage(bangobj, obs, collider, target,
                   Math.min(100, target.damage + damage), effect);
        }
    }

    /**
     * Damages the supplied piece by the specified amount, properly removing it
     * from the board if appropriate and reporting the specified effect.
     *
     * @param shooter the index of the player doing the damage or -1 if the
     * damage was not originated by a player.
     * @param newDamage the new total damage to assign to the damaged piece.
     */
    public static void damage (BangObject bangobj, Observer obs, int shooter,
                               Piece target, int newDamage, String effect)
    {
        // sanity check
        if (!target.isAlive()) {
            log.warning("Not damaging already dead target " +
                "[target=" + target.info() + ", shooter=" + shooter +
                ", nd=" + newDamage + ", effect=" + effect + "].");
            Thread.dumpStack();
            return;
        }

        // effect the actual damage
//         log.info("Damaging " + target.info() + " -> " + newDamage + ".");
        target.damage = newDamage;

        // report that the target was affected
        reportEffect(obs, target, effect);

        // if the target is not dead, we can stop here
        if (target.isAlive()) {
            return;
        }

        // report that the target was killed
        reportKill(obs, target);

        // flying targets must land when they die
        if (target.isFlyer()) {
            Point pt = bangobj.board.getOccupiableSpot(
                target.x, target.y, 5, new Random(bangobj.tick));
            if (pt != null) {
                moveAndReport(bangobj, target, pt.x, pt.y, obs);
            }
        }

        // if we have a shooter and we're on the server, record the kill
        if (shooter != -1 && bangobj.getManager().isManager(bangobj)) {
            // record the kill statistics
            bangobj.stats[shooter].incrementStat(Stat.Type.UNITS_KILLED, 1);
            bangobj.stats[target.owner].incrementStat(Stat.Type.UNITS_LOST, 1);
        }

        // if the should be removed when killed, do so now
        if (target.removeWhenDead()) {
            bangobj.removePieceDirect(target);
            reportRemoval(obs, target);
        }
    }

    /**
     * Initializes this effect (called only on the server) with the piece that
     * activated the bonus.
     */
    public void init (Piece piece)
    {
    }

    /**
     * Returns an array of pieces we must wait for before activating this
     * effect. The default implementation returns nothing, however, some
     * effects may require that we wait for a piece to finish animating before
     * starting but not necessarily block that piece from moving on to the next
     * animation once this effect's visualization is started.
     *
     * <p>Note: it is not necessary to return any pieces that are already
     * returned by {@link #getAffectedPieces} as those will automatically be
     * waited for.
     */
    public int[] getWaitPieces ()
    {
        return NO_PIECES;
    }

    /** Returns an array of the ids of all pieces affected by this effect. */
    public abstract int[] getAffectedPieces ();

    /**
     * Prepares this effect for application. This is executed on the server
     * before the effect is applied on the server and then distributed to the
     * client for application there. The effect should determine which pieces
     * it will impact as well as decide where it will be placing new pieces
     * (and update the board shadow to reflect those piece additions, though it
     * should not actually add the pieces until it is applied).
     *
     * @param dammap a mapping that should be used to record damage done
     * to a particular player's units (player index -> accumulated
     * damage).
     */
    public abstract void prepare (BangObject bangobj, IntIntMap dammap);

    /**
     * A method only used by effects created by cards that returns true if this
     * effect will serve its intended purpose or false if the card that caused
     * the effect should be considered unplayed and the effect cancelled. The
     * default implementation always assumes applicability.
     */
    public boolean isApplicable ()
    {
        return true;
    }

    /**
     * Applies this effect to the board and pieces. Any modifications to pieces
     * or the board should be made directly as this is executed on both the
     * client and server. <em>Note:</em> effects should always compute and
     * store the final result of their effects in {@link #prepare} and then
     * simply apply those results in {@link #apply} rather than do any
     * computation in {@link #apply} as we cannot rely on the values in the
     * piece during the apply to be the same as they would be on the server
     * when the effect is applied. The only truly safe time to inspect the
     * condition of the affected pieces is during {@link #prepare}.
     *
     * @param observer an observer to inform of piece additions, updates and
     * removals (for display purposes on the client). This may be null.
     */
    public abstract void apply (BangObject bangobj, Observer observer);

    /**
     * Creates an {@link EffectHandler} to manage the (potentially complicated)
     * visualization of this effect.
     */
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new EffectHandler();
    }

    @Override // documentation inherited
    public String toString ()
    {
        String cname = getClass().getName();
        return cname.substring(cname.lastIndexOf(".")+1) + ":" +
            super.toString();
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

    /** A helper function for moving a piece and reporting it. */
    protected static void moveAndReport (
        BangObject bangobj, Piece piece, int nx, int ny, Observer obs)
    {
        bangobj.board.clearShadow(piece);
        piece.position(nx, ny);
        bangobj.board.shadowPiece(piece);
        if (obs != null) {
            obs.pieceMoved(piece);
        }
    }

    /** A helper function for reporting piece death. */
    protected static void reportKill (Observer obs, Piece piece)
    {
        if (obs != null) {
            obs.pieceKilled(piece);
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

    /** Used by {@link #getWaitPieces}. */
    protected static final int[] NO_PIECES = new int[0];
}
