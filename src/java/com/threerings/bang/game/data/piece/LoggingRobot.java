//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.samskivert.util.ListUtil;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.BuzzsawShotEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.FetishEffect;
import com.threerings.bang.game.data.effect.ProximityShotEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.effect.TreeBedEffect;

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
    
    /** The two different logging robot unit types. */
    public static final String[] UNIT_TYPES = {
        "indian_post/logging_robot", "indian_post/locust_robot" };
    
    @Override // documentation inherited
    public int getTreeProximityDamage (TreeBed bed)
    {
        return TREE_PROXIMITY_DAMAGE[bed.growth];
    }
    
    @Override // documentation inherited
    public boolean isFlyer ()
    {
        return _type == LOCUST;
    }
    
    @Override // documentation inherited
    public boolean canActivateBonus (Bonus bonus)
    {
        BonusConfig bconfig = bonus.getConfig();
        return super.canActivateBonus(bonus) &&
            !(bconfig.holdable || bconfig.playersOnly);
    }
    
    @Override // documentation inherited
    public boolean validTarget (
        BangObject bangobj, Piece target, boolean allowSelf)
    {
        // locust robots can attack trees directly
        if (_type == LOCUST && target instanceof TreeBed && target.isAlive() &&
            ((TreeBed)target).growth > 0) {
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
        if (_type == LOCUST) {
            return new ShotEffect(this, target, damage,
                attackInfluenceIcons(), defendInfluenceIcons(target));
        } else {
            return new BuzzsawShotEffect(this, target, damage,
                attackInfluenceIcons(), defendInfluenceIcons(target));
        }       
    }
    
    @Override // documentation inherited
    public ArrayList<Effect> tick (
            short tick, BangObject bangobj, Piece[] pieces)
    {
        if (!isAlive() || _type == LOCUST) {
            return null;
        }
        ArrayList<Effect> effects = super.tick(tick, bangobj, pieces);
        ArrayList<ShotEffect> proxShots = new ArrayList<ShotEffect>();
        ProximityShotEffect proxShot = null;
        for (Piece piece : pieces) {
            if (getDistance(piece) != 1 || !piece.isAlive() ||
                !bangobj.board.canCross(x, y, piece.x, piece.y)) {
                continue;
            }
            if (_type == NORMAL && piece instanceof Unit &&
                    piece.owner != -1 && !piece.isAirborne()) {
                proxShot = addProxShot(
                        proxShot, proxShots, piece, UNIT_PROXIMITY_DAMAGE);
            } else if (piece instanceof TreeBed && 
                    ((TreeBed)piece).growth > 0) {
                TreeBed tb = (TreeBed)piece;
                proxShot = addProxShot(
                        proxShot, proxShots, tb, getTreeProximityDamage(tb));
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
            log.warning("Created logging robot of unknown type [type=" +
                config.type + "].");
            _type = NORMAL;
        }
    }
    
    /** The specific logging robot variety. */
    protected transient int _type;
    
    /** For each tree growth state, the amount by which logging robots next to
     * trees increase their damage. */
    public static final int[] TREE_PROXIMITY_DAMAGE = { 0, 20, 15, 10 };
    
    /** The base amount by which normal logging robots next to units damage
     * them with their rotating saw blades. */
    public static final int UNIT_PROXIMITY_DAMAGE = 5;
}
