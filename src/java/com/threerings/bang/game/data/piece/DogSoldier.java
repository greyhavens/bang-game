//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ProximityShotEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

import static com.threerings.bang.Log.log;

/**
 * Handles some special custom behavior needed for the Dog Soldier.
 */
public class DogSoldier extends Unit
{
    @Override // documentation inherited
    public boolean validTarget (
        BangObject bangobj, Piece target, boolean allowSelf)
    {
        return !target.isAirborne() &&
            super.validTarget(bangobj, target, allowSelf);
    }

    @Override // documentation inherited
    public boolean checkLineOfSight (
            BangBoard board, int tx, int ty, Piece target)
    {
        return board.canCross(tx, ty, target.x, target.y) &&
            super.checkLineOfSight(board, tx, ty, target);
    }

    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangBoard board, Piece[] pieces)
    {
        ArrayList<Effect> effects = super.tick(tick, board, pieces);
        ArrayList<ShotEffect> proxShots = new ArrayList<ShotEffect>();
        ProximityShotEffect proxShot = null;
        for (Piece piece : pieces) {
            if (piece.isTargetable() && piece.isAlive() && 
                    piece.owner != owner && !piece.isAirborne() && 
                    getDistance(piece) == 1 && 
                    board.canCross(x, y, piece.x, piece.y)) {
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

    @Override // documentation inherited
    public boolean canBePushed ()
    {
        return false;
    }

    /** The base amount by which dog soldiers next to units damage them with
     * their wild weapon wheeling. */
    public static final int UNIT_PROXIMITY_DAMAGE = 5;
}
