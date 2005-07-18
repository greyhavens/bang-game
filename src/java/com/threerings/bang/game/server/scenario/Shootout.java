//
// $Id$

package com.threerings.bang.game.server.scenario;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;

import com.threerings.util.MessageBundle;
import com.threerings.crowd.chat.server.SpeakProvider;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A gameplay scenario where the last player standing is the winner.
 */
public class Shootout extends Scenario
{
    @Override // documentation inherited
    public void gameWillStart (BangObject bangobj)
    {
        // create a fresh knockout array
        _knockoutOrder = new int[bangobj.players.length];
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        Piece[] pieces = bangobj.getPieceArray();

        // check to see whether anyone's pieces are still alive
        _havers.clear();
        for (int ii = 0; ii < pieces.length; ii++) {
            if ((pieces[ii] instanceof Unit) &&
                pieces[ii].isAlive()) {
                _havers.add(pieces[ii].owner);
            }
        }

        // score points for anyone who is knocked out as of this tick
        int score = IntListUtil.getMaxValue(_knockoutOrder) + 1;
        for (int ii = 0; ii < _knockoutOrder.length; ii++) {
            if (_knockoutOrder[ii] == 0 && !_havers.contains(ii)) {
                _knockoutOrder[ii] = score;
                bangobj.setPointsAt(bangobj.points[ii] + score, ii);
                String msg = MessageBundle.tcompose(
                    "m.knocked_out", bangobj.players[ii]);
                SpeakProvider.sendInfo(bangobj, GameCodes.GAME_MSGS, msg);
            }
        }

        // the game ends when one or zero players are left standing
        if (_havers.size() < 2) {
            // score points for the last player standing
            int winidx = _havers.get(0);
            bangobj.setPointsAt(bangobj.points[winidx] + score + 1, winidx);

            return true;
        }

        return false;
    }

    /** Used to calculate winners. */
    protected ArrayIntSet _havers = new ArrayIntSet();

    /** Used to track the order in which players are knocked out. */
    protected int[] _knockoutOrder;
}
