//
// $Id$

package com.threerings.bang.game.server.scenario;

import com.threerings.bang.game.data.BangObject;

/**
 * Implements a particular gameplay scenario.
 */
public abstract class Scenario
{
    /**
     * Called when a round is about to start.
     */
    public abstract void gameWillStart (BangObject bangobj);

    /**
     * Called at the start of every game tick to allow the scenario to
     * affect the game state and determine whether or not the game should
     * be ended.
     *
     * @return true if the game should be ended, false if not.
     */
    public abstract boolean tick (BangObject bangobj, short tick);
}
