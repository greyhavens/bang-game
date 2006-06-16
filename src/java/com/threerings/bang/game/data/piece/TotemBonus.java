//
// $Id$

package com.threerings.bang.game.data.piece;

/**
 * Special code to handle totem bonuses.
 */
public class TotemBonus extends Bonus
{
    /**
     * Convenience function to termine if a unit is holding a totem bonus.
     */
    public static boolean isHolding (Unit unit)
    {
        return (unit.holding != null && 
                unit.holding.startsWith("indian_post/totem"));
    }
}
