//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.HoldHandler;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Bonus;

import static com.threerings.bang.Log.log;

/**
 * Punt's a piece to an unoccupied surrounding tile.
 */
public class PuntEffect extends MoveEffect
{
    /** Piece id of the punter. */
    int causerId;

    public static PuntEffect puntBonus (
            BangObject bangobj, Bonus bonus, int causerId)
    {
        Point spot = bonus.getDropLocation(bangobj);
        if (spot == null) {
            log.warning("Unable to find spot to punt bonus", "bonus", bonus);
            return null;
        }
        PuntEffect effect = new PuntEffect();
        effect.init(bonus);
        effect.nx = (short)spot.x;
        effect.ny = (short)spot.y;
        effect.causerId = causerId;
        return effect;
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return new int[] { causerId };
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new HoldHandler();
    }
}
