//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Piece;

/**
 * The shot effect for the dream catcher's nightmare attack.
 */
public class NightmareShotEffect extends BallisticShotEffect
{
    public NightmareShotEffect (Piece shooter, Piece target, int damage,
                                String[] attackIcons, String[] defendIcons)
    {
        super(shooter, target, damage, attackIcons, defendIcons);
    }

    /** Constructor used when unserializing. */
    public NightmareShotEffect ()
    {
    }

    @Override // documentation inherited
    public boolean isDeflectable ()
    {
        return false;
    }
    
    @Override // documentation inherited
    public String getShotType ()
    {
        return "effects/indian_post/dream_catcher/nightmare";
    }
    
    @Override // documentation inherited
    public Trajectory getTrajectory ()
    {
        return Trajectory.FLAT;
    }
    
    @Override // documentation inherited
    protected String getEffect ()
    {
        return DAMAGED;
    }
}
