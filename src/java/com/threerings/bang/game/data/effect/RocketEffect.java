//
// $Id$

package com.threerings.bang.game.data.effect;

import com.jmex.bui.util.Point;

import com.threerings.bang.util.RenderUtil;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.client.RocketHandler;
import com.threerings.bang.game.client.EffectHandler;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Communicates that a ballistic shot was fired from one piece to another.
 */
public class RocketEffect extends Effect
{
    /** Types of projectile trajectories. */
    public enum Trajectory { FLAT, HIGH_ARC };

    /** The shooter. */
    public Piece shooter;

    /** The piece id of the target. */
    public int targetId;

    /** The new total damage to assign to the target. */
    public int newDamage;

    /** Ammount of damage being applied. */
    public int baseDamage;

    /** The x coordinates of the path this shot takes before finally
     * arriving at its target (not including the starting coordinate). */
    public short[] xcoords;

    /** The y coordinates of the path this shot takes before finally
     * arriving at its target (not including the starting coordinate). */
    public short[] ycoords;

    /** A secondary effect to apply before the shot. */
    public Effect[] preShotEffects = Piece.NO_EFFECTS;

    public RocketEffect (Piece shooter, Piece target, int damage)
    {
        this.shooter = shooter;
        targetId = target.pieceId;
        xcoords = append(xcoords, target.x);
        ycoords = append(ycoords, target.y);
        baseDamage = damage;
    }

    public RocketEffect (Piece shooter, Point p, int damage)
    {
        this.shooter = shooter;
        targetId = -1;
        xcoords = append(xcoords, (short)p.x);
        ycoords = append(ycoords, (short)p.y);
        baseDamage = damage;
    }

    /** Constructor used when unserializing. */
    public RocketEffect ()
    {
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new RocketHandler();
    }

    /**
     * Returns the type of shot fired, which could be a model or an effect.
     */
    public String getShotType ()
    {
        return "units/frontier_town/artillery/shell";
    }

    /**
     * Returns the type of trajectory that should be followed by the shot.
     */
    public Trajectory getTrajectory ()
    {
        return Trajectory.FLAT;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] pieces = (targetId == -1) ? new int[] { shooter.pieceId } : new int[] { shooter.pieceId, targetId };
        for (Effect effect : preShotEffects) {
            pieces = concatenate(pieces, effect.getAffectedPieces());
        }
        return pieces;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        if (targetId == -1) { // we were deflected into la la land, no problem
            return;
        }

        Piece target = bangobj.pieces.get(targetId);
        if (target != null) {
            // award a 20 damage point (2 game point) bonus for a kill
            int bonus = (newDamage == 100) ? 20 : 0;
            dammap.increment(target.owner, newDamage - target.damage + bonus);
            if (newDamage == 100) {
                Effect effect = target.willDie(bangobj, shooter.pieceId);
                if (effect != null) {
                    preShotEffects = ArrayUtil.append(preShotEffects, effect);
                }
            }
            for (Effect effect : preShotEffects) {
                effect.prepare(bangobj, dammap);
            }
        } else {
            log.warning("Shot effect missing target [id=" + targetId + "].");
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        // apply any secondary pre-shot effect
        for (Effect effect : preShotEffects) {
            effect.apply(bangobj, obs);
        }

        if (targetId == -1) {
            log.warning("Missing shot target " + this + ".");
            return false;
        }

        Piece target = bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing shot target " + this + ".");
            return false;
        }

        // finally do the damage
        return damage(bangobj, obs, shooter.owner, shooter, target, newDamage, "exploded");
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
}
