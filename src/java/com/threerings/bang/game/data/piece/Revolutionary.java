//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.data.UnitConfig;

/**
 * Handles the special capabilities of the Revolutionary unit..
 */
public class Revolutionary extends Unit
{
    @Override // documentation inherited
    public int adjustPieceAttack (Piece attacker, int damage)
    {
        damage = super.adjustPieceAttack(attacker, damage);
        // give other allied ground units a %10 attack bonus
        if (attacker.owner == owner && attacker.pieceId != pieceId &&
                attacker instanceof Unit && ((Unit)attacker).getConfig().mode 
                == UnitConfig.Mode.GROUND) {
            damage = (int)(damage * 1.1f);
        }
        return damage;
    }
}
