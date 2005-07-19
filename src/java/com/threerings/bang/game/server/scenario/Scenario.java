//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Implements a particular gameplay scenario.
 */
public abstract class Scenario
{
    /**
     * Called when a round is about to start.
     *
     * @return null if all is well, or a translatable string indicating
     * why the scenario is booched which will be displayed to the players
     * and the game will be cancelled.
     */
    public abstract String init (BangObject bangobj, ArrayList<Piece> markers);

    /**
     * Called at the start of every game tick to allow the scenario to
     * affect the game state and determine whether or not the game should
     * be ended.
     *
     * @return true if the game should be ended, false if not.
     */
    public abstract boolean tick (BangObject bangobj, short tick);

    /**
     * Called when a unit makes a move in the game but before the
     * associated update for that unit is broadcast. The scenario can make
     * further adjustments to the unit and modify other game data as
     * appropriate.
     */
    public void unitMoved (BangObject bangobj, Unit unit)
    {
    }
}
