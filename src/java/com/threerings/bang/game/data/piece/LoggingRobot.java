//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.FetishEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * The logging robot for the forest guardians scenario.
 */
public class LoggingRobot extends BallisticUnit
{
    @Override // documentation inherited
    public int getTreeProximityDamage ()
    {
        return BASE_TREE_PROXIMITY_DAMAGE;
    }
    
    @Override // documentation inherited
    public boolean canActivateBonus (Bonus bonus)
    {
        return false;
    }
    
    @Override // documentation inherited
    public boolean validTarget (Piece target, boolean allowSelf)
    {
        // logging robots can't see units holding the fox fetish
        return super.validTarget(target, allowSelf) &&
            (!(target instanceof Unit) ||
                !FetishEffect.FOX_FETISH.equals(((Unit)target).holding));
    }
    
    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangBoard board, Piece[] pieces)
    {
        ArrayList<Effect> effects = new ArrayList<Effect>();
        for (Piece piece : pieces) {
            if (piece instanceof Unit && getDistance(piece) == 1 &&
                piece.owner != -1 && !piece.isAirborne()) {
                effects.add(new ShotEffect(this, piece, UNIT_PROXIMITY_DAMAGE,
                    null, null));
            }
        }
        return effects.isEmpty() ? null : effects;
    }
    
    /** The base amount by which logging robots next to trees increase their
     * damage and encourage them to shrink. */
    public static final int BASE_TREE_PROXIMITY_DAMAGE = 20;
    
    /** The base amount by which logging robots next to units damage them with
     * their rotating saw blades. */
    public static final int UNIT_PROXIMITY_DAMAGE = 5;
}
