//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;
import java.util.List;

import com.samskivert.util.ListUtil;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.BuzzsawShotEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.FetishEffect;
import com.threerings.bang.game.data.effect.ProximityShotEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

import static com.threerings.bang.Log.*;

/**
 * The logging robot for the forest guardians scenario.
 */
public class LoggingRobot extends Unit
{
    /** Indicates that this robot is of the normal variety. */
    public static final int NORMAL = 0;

    /** Indicates that this robot is of the locust variety. */
    public static final int LOCUST = 1;

    /** Indicates that this robot is of the super variety. */
    public static final int SUPER = 2;

    /** Indicates that this robot is of the super locust variety. */
    public static final int SUPER_LOCUST = 3;

    /** The two different logging robot unit types. */
    public static final String[] UNIT_TYPES = {
        "indian_post/logging_robot", "indian_post/locust_robot",
        "indian_post/super_logging_robot", "indian_post/super_locust_robot" };

    /**
     * Returns the type of this robot ({@link #NORMAL}, {@link #LOCUST}, etc.)
     */
    public int getRobotType ()
    {
        return _type;
    }

    /**
     * Determines whether this robot is one of the "super" varieties.
     */
    public boolean isSuper ()
    {
        return (_type == SUPER || _type == SUPER_LOCUST);
    }

    @Override // documentation inherited
    public boolean isFlyer ()
    {
        return isLocust();
    }

    @Override // documentation inherited
    public boolean canActivateBonus (BangObject bangobj, Bonus bonus)
    {
        BonusConfig bconfig = bonus.getConfig();
        return super.canActivateBonus(bangobj, bonus) &&
            !(bconfig.holdable || bconfig.playersOnly);
    }

    @Override // documentation inherited
    public boolean validTarget (
        BangObject bangobj, Piece target, boolean allowSelf)
    {
        // locust robots can attack trees directly
        if (isAlive() && isLocust() && target instanceof TreeBed && target.isAlive() &&
            ((TreeBed)target).growth > 0 &&
            (getHindrance() == null || getHindrance().validTarget(this, target, allowSelf))) {
            return true;
        }
        // logging robots can't see units holding the fox fetish
        return super.validTarget(bangobj, target, allowSelf) &&
            (!(target instanceof Unit) ||
                !FetishEffect.FOX_FETISH.equals(((Unit)target).holding));
    }

    @Override // documentation inherited
    protected ShotEffect generateShotEffect (
            BangObject bangobj, Piece target, int damage)
    {
        if (isLocust()) {
            return new ShotEffect(this, target, damage,
                attackInfluenceIcons(), defendInfluenceIcons(target));
        } else {
            return new BuzzsawShotEffect(this, target, damage,
                attackInfluenceIcons(), defendInfluenceIcons(target));
        }
    }

    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangObject bangobj, List<Piece> pieces)
    {
        ArrayList<Effect> effects = super.tick(tick, bangobj, pieces);
        if (!isAlive() || isLocust()) {
            return effects;
        }

        ArrayList<ShotEffect> proxShots = new ArrayList<ShotEffect>();
        ProximityShotEffect proxShot = null;
        for (Piece piece : pieces) {
            if (getDistance(piece) != 1 || !piece.isAlive() ||
                !bangobj.board.canCross(x, y, piece.x, piece.y) ||
                // allow our hindrance to prevent proximity attacks
                (getHindrance() != null &&
                 !getHindrance().validTarget(this, piece, false))) {
                continue;
            }

            if (piece instanceof Unit && piece.owner != -1 &&
                !piece.isAirborne()) {
                proxShot = addProxShot(proxShot, proxShots, piece,
                    scaleProximityDamage(UNIT_PROXIMITY_DAMAGE));

            } else if (piece instanceof TreeBed &&
                       ((TreeBed)piece).growth > 0) {
                TreeBed tb = (TreeBed)piece;
                proxShot = addProxShot(proxShot, proxShots, tb,
                    scaleProximityDamage(TREE_PROXIMITY_DAMAGE));
            }
        }

        if (proxShot != null) {
            proxShot.proxShot = proxShots.toArray(new ShotEffect[0]);
            effects.add(proxShot);
        }
        return effects;
    }

    protected ProximityShotEffect addProxShot (ProximityShotEffect proxShot,
            ArrayList<ShotEffect> proxShots, Piece piece, int damage)
    {
        damage = piece.adjustProxDefend(this, damage);
        if (proxShot == null) {
            proxShot = new ProximityShotEffect(this, piece, damage, null, null);
        } else {
            proxShots.add(new ShotEffect(this, piece, damage, null, null));
        }
        return proxShot;
    }

    @Override // documentation inherited
    protected void init (UnitConfig config)
    {
        super.init(config);
        if ((_type = ListUtil.indexOf(UNIT_TYPES, config.type)) == -1) {
            log.warning("Created logging robot of unknown type", "type", config.type);
            _type = NORMAL;
        }
    }

    /**
     * Determines whether this robot is of the alternate, "locust" variety.
     */
    protected boolean isLocust ()
    {
        return (_type == LOCUST || _type == SUPER_LOCUST);
    }

    /**
     * Scales the given proximity damage according to the type of this robot.
     */
    protected int scaleProximityDamage (int damage)
    {
        return (_type == SUPER) ?
            (int)(damage * SUPER_PROXIMITY_DAMAGE_SCALE) : damage;
    }

    /** The specific logging robot variety. */
    protected transient int _type;

    /** The amount by which logging robots next to trees increase their
     * damage. */
    public static final int TREE_PROXIMITY_DAMAGE = 13;

    /** The base amount by which normal logging robots next to units damage
     * them with their rotating saw blades. */
    public static final int UNIT_PROXIMITY_DAMAGE = 5;

    /** A multiplier for super robots' proximity damage. */
    public static final float SUPER_PROXIMITY_DAMAGE_SCALE = 1.5f;
}
