//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.data.BonusConfig;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.FetishEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.effect.ProximityShotEffect;

/**
 * The logging robot for the forest guardians scenario.
 */
public class LoggingRobot extends BallisticUnit
{
    @Override // documentation inherited
    public int getTreeProximityDamage (TreeBed bed)
    {
        return TREE_PROXIMITY_DAMAGE[bed.growth];
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
        // logging robots can't see units holding the fox fetish
        return super.validTarget(bangobj, target, allowSelf) &&
            (!(target instanceof Unit) ||
                !FetishEffect.FOX_FETISH.equals(((Unit)target).holding));
    }
    
    @Override // documentation inherited
    public ArrayList<Effect> tick (
            short tick, BangObject bangobj, Piece[] pieces)
    {
        if (!isAlive()) {
            return null;
        }
        ArrayList<Effect> effects = super.tick(tick, bangobj, pieces);
        ArrayList<ShotEffect> proxShots = new ArrayList<ShotEffect>();
        ProximityShotEffect proxShot = null;
        for (Piece piece : pieces) {
            if (piece instanceof Unit && getDistance(piece) == 1 &&
                piece.owner != -1 && !piece.isAirborne() && piece.isAlive() &&
                bangobj.board.canCross(x, y, piece.x, piece.y)) {
                if (proxShot == null) {
                    proxShot = new ProximityShotEffect(this, piece,
                            UNIT_PROXIMITY_DAMAGE, null, null);
                } else {
                    proxShots.add(new ShotEffect(this, piece, 
                                UNIT_PROXIMITY_DAMAGE, null, null));
                }
            }
        }
        if (proxShot != null) {
            proxShot.proxShot = proxShots.toArray(new ShotEffect[0]);
            effects.add(proxShot);
        }
        return effects;
    }
    
    /** For each tree growth state, the amount by which logging robots next to
     * trees increase their damage. */
    public static final int[] TREE_PROXIMITY_DAMAGE = { 0, 20, 15, 10 };
    
    /** The base amount by which logging robots next to units damage them with
     * their rotating saw blades. */
    public static final int UNIT_PROXIMITY_DAMAGE = 5;
}
