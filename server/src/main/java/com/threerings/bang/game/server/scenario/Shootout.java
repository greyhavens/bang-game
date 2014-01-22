//
// $Id$

package com.threerings.bang.game.server.scenario;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;

import com.threerings.crowd.chat.server.SpeakUtil;
import com.threerings.presents.server.InvocationException;
import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PieceSet;

/**
 * A gameplay scenario where the last player standing is the winner.
 */
public class Shootout extends Scenario
{
    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // create a fresh knockout array
        _knockoutOrder = new int[bangobj.players.length];
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        boolean validate = super.tick(bangobj, tick);

        // check to see whether anyone's pieces are still alive
        _havers.clear();
        for (Piece p : bangobj.getPieceArray()) {
            if ((p instanceof Unit) && p.isAlive()) {
                _havers.add(p.owner);
            }
        }

        // score points for anyone who is knocked out as of this tick
        int score = IntListUtil.getMaxValue(_knockoutOrder) + 1;
        for (int ii = 0; ii < _knockoutOrder.length; ii++) {
            if (_knockoutOrder[ii] == 0 && !_havers.contains(ii)) {
                _knockoutOrder[ii] = score;
                bangobj.grantPoints(ii, SCORE_POINTS * score);
                String msg = MessageBundle.tcompose(
                    "m.knocked_out", bangobj.players[ii]);
                SpeakUtil.sendInfo(bangobj, GameCodes.GAME_MSGS, msg);
            }
        }

        // the game ends when one or zero players are left standing
        if (_havers.size() < 2) {
            // score points for the last player standing
            bangobj.grantPoints(_havers.get(0), SCORE_POINTS * (score + 1));
            // set the last tick to now to end the round
            bangobj.setLastTick(tick);
        }

        return validate;
    }

    /** Used to calculate winners. */
    protected ArrayIntSet _havers = new ArrayIntSet();

    /** Used to track the order in which players are knocked out. */
    protected int[] _knockoutOrder;

    protected static final int SCORE_POINTS = 50;
}
