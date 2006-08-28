//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Piece;

/**
 * The shot effect for the logging robot's buzzsaw attack.
 */
public class BuzzsawShotEffect extends BallisticShotEffect
{
    public BuzzsawShotEffect (Piece shooter, Piece target, int damage,
                              String[] attackIcons, String[] defendIcons)
    {
        super(shooter, target, damage, attackIcons, defendIcons);
    }

    /** Constructor used when unserializing. */
    public BuzzsawShotEffect ()
    {
    }

    @Override // documentation inherited
    public String getShotType ()
    {
        return "units/indian_post/logging_robot/buzzsaw";
    }
    
    @Override // documentation inherited
    protected String getEffect ()
    {
        return DAMAGED;
    }
}
