//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.client.InstantShotHandler;
import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Communicates that a shot was fired from one piece to another.
 */
public class ShotEffect extends Effect
{
    /** Indicates that the target was damaged by a normal shot. */
    public static final String DAMAGED = "bang";

    /** Indicates that the target was damaged by a ballistic shot. */
    public static final String EXPLODED = "exploded";

    /** Indicates that the target was damaged by a ballistic shot. */
    public static final String ROCKET_BURST = "rocket_burst";

    /** We also rotate the shooter, thereby affecting it. */
    public static final String ROTATED = "rotated";

    /** Indicates that a shooter shot without moving. */
    public static final String SHOT_NOMOVE = "shot_nomove";

    /** Indication that the shot was a dud. */
    public static final String DUDDED = "dudded";

    /** Indication that the shot misfired. */
    public static final String MISFIRED = "misfired";

    /** A normal shot. */
    public static final int NORMAL = 0;

    /** A return fire shot. */
    public static final int RETURN_FIRE = 1;

    /** A collateral damage shot. */
    public static final int COLLATERAL_DAMAGE = 2;

    /** A dud shot. */
    public static final int DUD = 3;

    /** A misfired shot. */
    public static final int MISFIRE = 4;

    /** A proximity shot. */
    public static final int PROXIMITY = 5;

    /** Maps shot types to animation identifiers. */
    public static final String[] SHOT_ACTIONS = {
        "shooting", "returning_fire", "collateral_damage",
        "dud", "misfire", "proximity"
    };

    /** The type of shot. */
    public short type = NORMAL;

    /** The piece id of the shooter. */
    public int shooterId;

    /** Used to update the last acted tick of the shooter if appropriate. */
    public short shooterLastActed = -1;

    /** The piece id of the target. */
    public int targetId;

    /** The new total damage to assign to the target. */
    public int newDamage;

    /** An adjusted last acted time to apply to the target. */
    public short targetLastActed = -1;

    /** The x coordinates of the path this shot takes before finally
     * arriving at its target (not including the starting coordinate). */
    public short[] xcoords;

    /** The y coordinates of the path this shot takes before finally
     * arriving at its target (not including the starting coordinate). */
    public short[] ycoords;

    /** When non-null, the piece ids of the units deflecting the shot at
     * each coordinate. */
    public short[] deflectorIds;

    /** A secondary effect to apply before the shot. */
    public Effect[] preShotEffects = Piece.NO_EFFECTS;

    /** Ammount of damage being applied. */
    public int baseDamage;

    /** Attack influence icon name. */
    public String[] attackIcons;

    /** Defend influence icon name. */
    public String[] defendIcons;

    /** Direction location the target is pushed. */
    public short pushx = -1, pushy = -1;

    /** If the target should use the push animation. */
    public boolean pushAnim = true;

    /**
     * Constructor used when creating a new shot effect.
     *
     * @param damage the amount by which to increase the target's damage.
     * This will be capped and converted to an absolute value.
     */
    public ShotEffect (Piece shooter, Piece target, int damage,
                       String[] attackIcons, String[] defendIcons)
    {
        shooterId = shooter.pieceId;
        setTarget(target, damage, attackIcons, defendIcons);
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
    public void setTarget (Piece target, int damage,
                           String[] attackIcons, String[] defendIcons)
    {
        if (targetId > 0) {
            deflectorIds = append(deflectorIds, (short)targetId);
        }
        targetId = target.pieceId;
        baseDamage = Math.max(damage, 0);
        this.attackIcons = attackIcons;
        this.defendIcons = defendIcons;
        newDamage = Math.max(0, Math.min(100, target.damage + damage));
        xcoords = append(xcoords, target.x);
        ycoords = append(ycoords, target.y);
    }

    /**
     * Determines whether the shot can be deflected.
     */
    public boolean isDeflectable ()
    {
        return false;
    }

    /**
     * Deflects this shot away from its original target, clearing out the
     * target information and appending the specified coordinates onto the
     * shot's path. It is assumed that there is no piece at those
     * coordinates.
     */
    public void deflectShot (short x, short y)
    {
        deflectorIds = append(deflectorIds, (short)targetId);
        targetId = -1;
        newDamage = 0;
        xcoords = append(xcoords, x);
        ycoords = append(ycoords, y);
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] pieces = new int[] { shooterId, targetId };
        for (Effect effect : preShotEffects) {
            pieces = concatenate(pieces, effect.getAffectedPieces());
        }
        if (deflectorIds != null) {
            for (short deflectorId : deflectorIds) {
                pieces = ArrayUtil.append(pieces, deflectorId);
            }
        }
        return pieces;
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        int[] waiters = NO_PIECES;
        for (Effect effect : preShotEffects) {
            waiters = concatenate(waiters, effect.getWaitPieces());
        }
        return waiters;
    }

    @Override // documentation inherited
    public Rectangle[] getBounds (BangObject bangobj)
    {
        Rectangle rect = (pushx > -1) ?
        new Rectangle(pushx, pushy, 1, 1) : null;
        int idx = xcoords.length - 1;
        if (rect == null) {
            rect = new Rectangle(xcoords[idx], ycoords[idx], 0, 0);
        } else {
            rect.add(xcoords[idx], ycoords[idx]);
        }
        rect.width++;
        rect.height++;
        return new Rectangle[] { rect };
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
                Effect effect = target.willDie(bangobj, shooterId);
                if (effect != null) {
                    preShotEffects = ArrayUtil.append(preShotEffects, effect);
                }
            } else {
                Piece shooter = bangobj.pieces.get(shooterId);
                if (shooter != null) {
                    preShotEffects = ArrayUtil.concatenate(preShotEffects,
                        shooter.willShoot(bangobj, target, this));
                } else {
                    log.warning("Shot effect missing shooter", "id", shooterId);
                }
            }
            for (Effect effect : preShotEffects) {
                effect.prepare(bangobj, dammap);
            }
        } else {
            log.warning("Shot effect missing target", "id", targetId);
        }
    }

    /**
     * This is called on the client by the shot handler to apply any unit state
     * changes needed <em>prior</em> to the shot animation. The normal effect
     * application takes place <em>after</em> the shot has been animated.
     */
    public void preapply (BangObject bangobj, Observer obs)
    {
        Unit shooter = (Unit)bangobj.pieces.get(shooterId);

        // rotate the shooter to face the target if this not collateral damage
        if (type != COLLATERAL_DAMAGE) {
            reportEffect(obs, shooter, ROTATED);
        }

        // update the shooter's last acted if necessary
        if (shooter != null && shooterLastActed != -1 &&
            shooter.lastActed != shooterLastActed) {
            shooter.lastActed = shooterLastActed;
            reportEffect(obs, shooter, SHOT_NOMOVE);
            shooterLastActed = -1; // avoid doing this again in apply()
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        // apply any secondary pre-shot effect
        for (Effect effect : preShotEffects) {
            effect.apply(bangobj, obs);
        }

        // update the shooter's last acted if necessary
        Unit shooter = (Unit)bangobj.pieces.get(shooterId);
        if (shooter == null) {
            log.warning("Missing shooter " + this + ".");
            return false;
        }
        if (shooterLastActed != -1 && shooter.lastActed != shooterLastActed) {
            shooter.lastActed = shooterLastActed;
            reportEffect(obs, shooter, SHOT_NOMOVE);
        }

        return applyTarget(bangobj, shooter, obs);
    }

    /**
     * Appends an attack/defend icon onto the list.
     */
    public void appendIcon (String icon, boolean attack)
    {
        if (icon == null) {
            return;
        }
        String[] icons = (attack ? attackIcons : defendIcons);
        if (icons == null) {
            icons = new String[] { icon };
        } else {
            icons = ArrayUtil.append(icons, icon);
        }
        if (attack) {
            attackIcons = icons;
        } else {
            defendIcons = icons;
        }
    }

    /**
     * Apply the shot to the target.
     */
    protected boolean applyTarget (
            BangObject bangobj, Unit shooter, Observer obs)
    {
        // if we were deflected into la la land, we can stop here
        if (targetId == -1) {
            return true;
        }
        Piece target = bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing shot target " + this + ".");
            return false;
        }

        // if we have a new last acted to assign to the target, do that
        if (targetLastActed != -1) {
            reportEffect(obs, target, targetLastActed > target.lastActed ?
                AdjustTickEffect.STARED_DOWN : AdjustTickEffect.GIDDY_UPPED);
            target.lastActed = targetLastActed;
        }

        // finally do the damage
        String effect = getEffect();
        if (pushx != -1) {
            return collide(bangobj, obs, shooter.owner, shooter, target,
                newDamage, pushx, pushy, effect);
        }
        return damage(bangobj, obs, shooter.owner, shooter, target, newDamage,
            effect);
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new InstantShotHandler();
    }

    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return baseDamage;
    }

    /** The effect string. */
    protected String getEffect ()
    {
        return DAMAGED;
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
