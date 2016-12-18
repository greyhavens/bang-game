//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ExpireHindranceEffect;
import com.threerings.bang.game.data.effect.ExpireInfluenceEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * Represents a temporary hindrance on a unit.  Hindrances can adjust
 * a unit's various basic properties and they can expire after a certain
 * number of ticks.
 */
public abstract class Hindrance extends Influence
{
    @Override // documentation inherited
    public ExpireInfluenceEffect createExpireEffect ()
    {
        return new ExpireHindranceEffect();
    }
    
    /**
     * Create a specialized ShotEffect that will override the standard
     * Piece.shoot value;
     */
    public ShotEffect shoot (
            BangObject bangobj, Unit shooter, Piece target, float scale)
    {
        return null;
    }

    /**
     * Gives the hindrance a chance to generate an effect after the affected
     * unit has moved of its own volition.
     */
    public Effect maybeGeneratePostMoveEffect (int steps)
    {
        return null;
    }
    
    /**
     * Gives the hindrance a chance to generate an effect after the affected
     * unit has been ordered to move/shoot.
     */
    public Effect maybeGeneratePostOrderEffect ()
    {
        return null;
    }
    
    /**
     * Called on both client and server to indicate that the piece moved of
     * its own volition.
     */
    public void didMove (int steps, short tick)
    {
    }
    
    /**
     * Allows the hindrance to generate an effect on the tick.
     */
    public Effect tick ()
    {
        return null;
    }
    
    /**
     * Perform an effect on the target after shooting.
     */
    public Effect affectTarget (Piece target)
    {
        return null;
    }

    /**
     * Determines if this is a valid target.
     */
    public boolean validTarget (Unit shooter, Piece target, boolean allowSelf)
    {
        return true;
    }

    /**
     * Called when a unit is damaged.
     */
    public void wasDamaged (int newDamage)
    {
        // nothing doing
    }

    /**
     * Determines if this hindrance will visibly affect the unit.
     */
    public boolean isVisible ()
    {
        return false;
    }
}
