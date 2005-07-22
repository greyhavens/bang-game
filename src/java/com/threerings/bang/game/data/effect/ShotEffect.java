//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Communicates that a shot was fired from one piece to another.
 */
public class ShotEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String DAMAGED = "bang";

    /** The piece id of the shooter. */
    public int shooterId;

    /** The piece id of the target. */
    public int targetId;

    /** The new total damage to assign to the target. */
    public int damage;

    /** An adjusted last acted time to apply to the target. */
    public short newLastActed = -1;

    /** Constructor used when unserializing. */
    public ShotEffect ()
    {
    }

    /**
     * Constructor used when creating an effect.
     *
     * @param damage the amount by which to increase the target's damage.
     */
    public ShotEffect (Piece shooter, Piece target, int damage)
    {
        shooterId = shooter.pieceId;
        targetId = target.pieceId;
        this.damage = Math.min(100, target.damage + damage);
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing target during apply!? [id=" + targetId + "].");
        } else {
            dammap.increment(target.owner, damage - target.damage);
        }
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing shot target " + this + ".");
            return;
        }
        if (newLastActed != -1) {
            target.lastActed = newLastActed;
        }
        damage(bangobj, obs, target, damage, DAMAGED);
    }

    /**
     * Damages the supplied piece by the specified amount, properly
     * removing it from the board if appropriate and reporting the
     * specified effect.
     *
     * @param damage the new total damage to assign to the damaged piece.
     */
    public static void damage (BangObject bangobj, Observer obs, Piece target,
                               int damage, String effect)
    {
        // effect the actual damage
        log.info("Damaging " + target.info() + " -> " + damage + ".");
        target.damage = damage;

        // report that the target was affected
        reportEffect(obs, target, effect);

        // if the target is dead and should be removed, do so
        if (!target.isAlive() && target.removeWhenDead()) {
            bangobj.removePieceDirect(target);
            reportRemoval(obs, target);
        }
    }
}
