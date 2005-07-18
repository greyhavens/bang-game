//
// $Id$

package com.threerings.bang.game.server.scenario;

import com.threerings.bang.game.data.BangObject;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li> Each player has a mine shaft, and those mine shafts start with a
 * particular quantity of gold.
 * <li> When another player's unit lands on (or in front of) the mine
 * shaft, they steal a nugget of gold from the shaft and must return that
 * nugget to their own shaft to deposit it.
 * <li> If the unit carrying the nugget is shot, it drops the nugget in a
 * nearby square and the nugget can then be picked up by any piece that
 * lands on it.
 * <li> When one player's mine is completely depleted of nuggets, the
 * round ends.
 * <li> Any units that are killed during the round respawn near the
 * player's starting marker.
 * </ul>
 */
public class ClaimJumping extends Scenario
{
    @Override // documentation inherited
    public void gameWillStart (BangObject bangobj)
    {
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        return false;
    }
}
