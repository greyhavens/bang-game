//
// $Id$

package com.samskivert.bang.data.effect;

import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.piece.Piece;

import static com.samskivert.bang.Log.log;

/**
 * Communicates that a shot was fired from one piece to another.
 */
public class ShotEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String DAMAGED = "bang";

    public int shooterId;

    public int targetId;

    public int damage;

    public void prepare (BangObject bangobj)
    {
        // nada
    }

    public void apply (BangObject bangobj, Observer obs)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing shot target " + this + ".");
            return;
        }
        damage(bangobj, obs, target, damage, DAMAGED);
    }

    /**
     * Damages the supplied piece by the specified amount, properly
     * removing it from the board if appropriate and reporting the
     * specified effect.
     */
    public static void damage (BangObject bangobj, Observer obs, Piece target,
                               int damage, String effect)
    {
        // effect the actual damage
        target.damage = Math.min(100, target.damage + damage);

        // report that the target was affected
        reportEffect(obs, target, effect);

        // if the target is dead and should be removed, do so
        if (!target.isAlive() && target.removeWhenDead()) {
            bangobj.pieces.removeDirect(target);
            bangobj.board.updateShadow(target, null);
            reportRemoval(obs, target);
        }
    }
}
