//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.util.PieceUtil;

import static com.threerings.bang.Log.log;

/**
 * Communicates that a shot was fired from one piece to another.
 */
public class ShotEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String DAMAGED = "bang";

    /** We also rotate the shooter, thereby affecting it. */
    public static final String ROTATED = "rotated";

    /** A normal shot. */
    public static final int NORMAL = 0;

    /** A return fire shot. */
    public static final int RETURN_FIRE = 1;

    /** A collateral damage shot. */
    public static final int COLLATERAL_DAMAGE = 2;

    /** Maps shot types to animation identifiers. */
    public static final String[] SHOT_ACTIONS = {
        "shooting", "returning_fire", "collateral_damage"
    };

    /** The type of shot. */
    public short type = NORMAL;

    /** The piece id of the shooter. */
    public int shooterId;

    /** The piece id of the target. */
    public int targetId;

    /** The new total damage to assign to the target. */
    public int newDamage;

    /** An adjusted last acted time to apply to the target. */
    public short newLastActed = -1;

    /** The x coordinates of the path this shot takes before finally
     * arriving at its target (not including the starting coordinate). */
    public short[] xcoords;

    /** The y coordinates of the path this shot takes before finally
     * arriving at its target (not including the starting coordinate). */
    public short[] ycoords;

    /**
     * Constructor used when creating a new shot effect.
     *
     * @param damage the amount by which to increase the target's damage.
     * This will be capped and converted to an absolute value.
     */
    public ShotEffect (Piece shooter, Piece target, int damage)
    {
        shooterId = shooter.pieceId;
        setTarget(target, damage);
    }

    /** Constructor used when unserializing. */
    public ShotEffect ()
    {
    }

    /**
     * Configures this shot effect with a target and a damage amount. Any
     * previous target will be overridden and the new target's coordinates
     * will be added onto the shot's path.
     *
     * @param damage the amount by which to increase the target's damage.
     * This will be capped and converted to an absolute value.
     */
    public void setTarget (Piece target, int damage)
    {
        targetId = target.pieceId;
        newDamage = Math.min(100, target.damage + damage);
        xcoords = append(xcoords, target.x);
        ycoords = append(ycoords, target.y);
    }

    /**
     * Deflects this shot away from its original target, clearing out the
     * target information and appending the specified coordinates onto the
     * shot's path. It is assumed that there is no piece at those
     * coordinates.
     */
    public void deflectShot (short x, short y)
    {
        targetId = -1;
        newDamage = 0;
        xcoords = append(xcoords, x);
        ycoords = append(ycoords, y);
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        if (targetId == -1) { // we were deflected into la la land, no problem
            return;
        }

        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target != null) {
            dammap.increment(target.owner, newDamage - target.damage);
        } else {
            log.warning("Shot effect missing target [id=" + targetId + "].");
        }
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        if (targetId == -1) { // we were deflected into la la land, no problem
            return;
        }

        // rotate the shooter to face the target
        Piece shooter = (Piece)bangobj.pieces.get(shooterId);
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (shooter != null && target != null) {
            short orient = PieceUtil.getDirection(shooter, target);
            shooter.orientation = orient;
            reportEffect(obs, shooter, ROTATED);
        }

        if (target == null) {
            log.warning("Missing shot target " + this + ".");
            return;
        }
        if (newLastActed != -1) {
            target.lastActed = newLastActed;
        }
        damage(bangobj, obs, shooter.owner, target, newDamage, DAMAGED);
    }

    /** Helper function for setting targets. */
    protected short[] append (short[] array, short value)
    {
        short[] narray;
        if (array == null) {
            narray = new short[] { value };
        } else {
            narray = new short[array.length+1];
            System.arraycopy(array, 0, narray, 0, array.length);
            narray[array.length] = value;
        }
        return narray;
    }

    /**
     * Damages the supplied piece by the specified amount, properly
     * removing it from the board if appropriate and reporting the
     * specified effect.
     *
     * @param shooter the index of the player doing the damage or -1 if
     * the damage was not originated by a player.
     * @param newDamage the new total damage to assign to the damaged piece.
     */
    public static void damage (BangObject bangobj, Observer obs, int shooter,
                               Piece target, int newDamage, String effect)
    {
        // effect the actual damage
        log.fine("Damaging " + target.info() + " -> " + newDamage + ".");
        target.damage = newDamage;

        // report that the target was affected
        reportEffect(obs, target, effect);

        // if the target is dead and we have a shooter and we're on the
        // server, report the kill
        if (shooter != -1 && !target.isAlive() &&
            bangobj.getManager().isManager(bangobj)) {
            // the manager will listen for this event and record the kill
            bangobj.setKiller(shooter);
        }

        // if the target is dead and should be removed, do so
        if (!target.isAlive() && target.removeWhenDead()) {
            bangobj.removePieceDirect(target);
            reportRemoval(obs, target);
        }
    }
}
