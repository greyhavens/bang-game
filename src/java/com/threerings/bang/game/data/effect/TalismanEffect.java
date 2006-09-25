//
// $Id$

package com.threerings.bang.game.data.effect;

/**
 * Causes the activating piece to "pick up" a talisman.
 */
public class TalismanEffect extends HoldEffect
{
    /** The bonus identifier for the talisman bonus. */
    public static final String TALISMAN_BONUS = "indian_post/talisman";

    /** Indicates that we picked up a talisman. */
    public static final String PICKED_UP_TALISMAN =
        "indian_post/talisman/pickedup";
        
    /** Indicates that we dropped a talisman. */
    public static final String DROPPED_TALISMAN =
        "indian_post/talisman/dropped";
        
    public TalismanEffect ()
    {
        type = TALISMAN_BONUS;
    }
    
    @Override // documentation inherited
    public String getPickedUpEffect ()
    {
        return PICKED_UP_TALISMAN;
    }
    
    @Override // documentation inherited
    public String getDroppedEffect ()
    {
        return DROPPED_TALISMAN;
    }
    
    /** Add the dropped effect to the superclass's static list. */
    static {
        _droppedEffects.add(DROPPED_TALISMAN);
    }
}
