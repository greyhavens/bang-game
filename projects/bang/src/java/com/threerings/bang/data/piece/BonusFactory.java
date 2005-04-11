//
// $Id$

package com.threerings.bang.data.piece;

import java.util.ArrayList;

/**
 * Knows about all possible bonuses and does the necessary math to decide
 * which bonus will be deployed when the game wants to do so.
 */
public class BonusFactory
{
    public BonusFactory ()
    {
    }

    protected ArrayList<Bonus> _bonuses = new ArrayList<Bonus>();
}
