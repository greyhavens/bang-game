//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;

/**
 * The logging robot for the forest guardians scenario.
 */
public class LoggingRobot extends BallisticUnit
{
    @Override // documentation inherited
    public int getTreeProximityDamage ()
    {
        return BASE_TREE_PROXIMITY_DAMAGE;
    }
    
    /** The base amount by which logging robots next to trees increase their
     * damage and encourage them to shrink. */
    public static final int BASE_TREE_PROXIMITY_DAMAGE = 20;
}
