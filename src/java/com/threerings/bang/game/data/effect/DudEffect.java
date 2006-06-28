//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * An effect that causes the next shot by a piece to have no effect.
 */
public class DudEffect extends SetHindranceEffect
{
    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return super.isApplicable() && unit.getConfig().gunUser;
    }

    @Override // documentation inherited
    protected Hindrance createHindrance (Unit target)
    {
        return new Hindrance() {
            public String getName () {
                return null;
            }

            public ShotEffect shoot (BangObject bangobj, Unit shooter, 
                    Piece target, float scale)
            {
                _expired = true;
                return new FailedShotEffect(shooter, target, 0);
            }

            public boolean isExpired (short tick)
            {
                return _expired;
            }

            boolean _expired = false;
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "dud";
    }
}
