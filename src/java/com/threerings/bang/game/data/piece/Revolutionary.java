//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * Handles the special capabilities of the Revolutionary unit..
 */
public class Revolutionary extends Unit
{
    @Override // documentation inherited
    public int adjustPieceAttack (
            Piece attacker, int damage, ArrayList<String> attackIcons)
    {
        damage = super.adjustPieceAttack(attacker, damage, attackIcons);
        // give other allied ground units a %10 attack bonus
        if (attacker.owner == owner && attacker.pieceId != pieceId &&
                attacker instanceof Unit && ((Unit)attacker).getConfig().mode 
                == UnitConfig.Mode.GROUND) {
            damage = (int)(damage * 1.1f);
            attackIcons.add("revolutionary_bonus");
        }
        return damage;
    }

    @Override // documentation inherited
    protected ShotEffect unitShoot (
            BangObject bangobj, Piece target, float scale)
    {
        // if he can use his sword he gets an attack bonus
        boolean proximity = false;
        if (getDistance(target) == 1 && !target.isAirborne() &&
                bangobj.board.canCross(x, y, target.x, target.y)) {
            scale *= SWORD_ATTACK_BONUS;
            proximity = true;
        }
        ShotEffect shot = super.unitShoot(bangobj, target, scale);
        if (shot != null && proximity) {
            shot.type = ShotEffect.PROXIMITY;
            shot.appendIcon("revolutionary", true);
        }
        return shot;
    }

    protected static final float SWORD_ATTACK_BONUS = 1.5f;
}
