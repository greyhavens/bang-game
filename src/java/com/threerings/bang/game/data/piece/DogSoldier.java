//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;

import static com.threerings.bang.Log.log;

/**
 * Handles some special custom behavior needed for the Dog Soldier.
 */
public class DogSoldier extends Unit
{
    @Override // documentation inherited
    public boolean validTarget (
        BangObject bangobj, Piece target, boolean allowSelf)
    {
        return !target.isAirborne() &&
            super.validTarget(bangobj, target, allowSelf);
    }

    @Override // documentation inherited
    public boolean checkLineOfSight (
            BangBoard board, int tx, int ty, Piece target)
    {
        return board.canCross(tx, ty, target.x, target.y) &&
            super.checkLineOfSight(board, tx, ty, target);
    }
}
