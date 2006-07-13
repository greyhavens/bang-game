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

    public TalismanEffect ()
    {
        type = TALISMAN_BONUS;
    }
}
