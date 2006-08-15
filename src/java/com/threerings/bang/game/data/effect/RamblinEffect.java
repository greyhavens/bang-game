//
// $Id$

package com.threerings.bang.game.data.effect;

/**
 * An effect that causes the piece in question to ramble along and move one
 * square further until it is killed and respawned.
 */
public class RamblinEffect extends AdjustMoveInfluenceEffect
{
    public RamblinEffect ()
    {
        super(1, -1);
        icon = "ramblin";
        name = "frontier_town/ramblin";
    }
}
