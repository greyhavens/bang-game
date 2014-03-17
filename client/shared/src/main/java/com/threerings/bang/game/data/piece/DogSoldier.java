//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;
import java.util.List;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ProximityShotEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * Handles some special custom behavior needed for the Dog Soldier.
 */
public class DogSoldier extends Unit
{
    @Override // documentation inherited
    public boolean validTarget (BangObject bangobj, Piece target, boolean allowSelf, boolean prox)
    {
        return !target.isAirborne() && super.validTarget(bangobj, target, allowSelf, prox);
    }

    @Override // documentation inherited
    public boolean checkLineOfSight (BangBoard board, int tx, int ty, Piece target)
    {
        return board.canCross(tx, ty, target.x, target.y) &&
            super.checkLineOfSight(board, tx, ty, target);
    }

    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangObject bangobj, List<Piece> pieces)
    {
        if (!isAlive()) {
            return null;
        }

        ArrayList<Effect> effects = super.tick(tick, bangobj, pieces);
        ArrayList<ShotEffect> proxShots = new ArrayList<ShotEffect>();
        ProximityShotEffect proxShot = null;
        for (Piece piece : pieces) {
            if (validTarget(bangobj, piece, false, true)) {
                int damage = piece.adjustProxDefend(this, UNIT_PROXIMITY_DAMAGE);
                if (proxShot == null) {
                    proxShot = new ProximityShotEffect(this, piece, damage, null, null);
                } else {
                    proxShots.add(new ShotEffect(this, piece, damage, null, null));
                }
            }
        }
        if (proxShot != null) {
            proxShot.proxShot = proxShots.toArray(new ShotEffect[0]);
            effects.add(proxShot);
        }
        return effects;
    }

    @Override // documentation inherited
    public boolean canBePushed ()
    {
        return false;
    }

    /** The base amount by which dog soldiers next to units damage them with
     * their wild weapon wheeling. */
    public static final int UNIT_PROXIMITY_DAMAGE = 7;
}
