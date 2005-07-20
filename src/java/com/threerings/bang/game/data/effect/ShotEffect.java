//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

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

    public Piece drop;

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing target during apply!? [id=" + targetId + "].");
        } else {
            dammap.increment(target.owner, Math.min(damage, 100-target.damage));
        }

        // if this piece is benuggeted, force it to drop its nugget
        if (target instanceof Unit && ((Unit)target).benuggeted) {
            // find a place to drop our nugget
            Point spot = bangobj.board.getOccupiableSpot(target.x, target.y, 3);
            if (spot == null) {
                log.info("Can't find anywhere to drop nugget " +
                         "[target=" + target + "].");
            } else {
                drop = Bonus.createBonus(BonusConfig.getConfig("nugget"));
                drop.assignPieceId();
                drop.position(spot.x, spot.y);
                bangobj.board.updateShadow(null, drop);
            }
        }
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        // add our dropped piece if we have one
        if (drop != null) {
            bangobj.addPieceDirect(drop);
            reportAddition(obs, drop);
        }

        // damage our target
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing shot target " + this + ".");
            return;
        }
        if (drop != null && target instanceof Unit) {
            // note that it dropped its nugget
            ((Unit)target).benuggeted = false;
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
        log.info("Damaging " + target.info() + " by " + damage +
                 " points, resulting in " + target.damage + ".");

        // report that the target was affected
        reportEffect(obs, target, effect);

        // if the target is dead and should be removed, do so
        if (!target.isAlive() && target.removeWhenDead()) {
            bangobj.removePieceDirect(target);
            reportRemoval(obs, target);
        }
    }
}
