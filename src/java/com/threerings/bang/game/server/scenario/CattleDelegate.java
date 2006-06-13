//
// $Id$

package com.threerings.bang.game.server.scenario;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Train;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Handles the behavior of cattle.
 */
public class CattleDelegate extends ScenarioDelegate
{
    @Override // documentation inherited
    public void pieceMoved (BangObject bangobj, Piece piece)
    {
        if (piece instanceof Unit) {
            checkSpookedCattle(bangobj, (Unit)piece);

        } else if (piece instanceof Train &&
                   ((Train)piece).type == Train.ENGINE) {
            checkSpookedCattle(bangobj, piece);
        }
    }

    protected void checkSpookedCattle (BangObject bangobj, Piece spooker)
    {
        // check to see if this piece spooked any cattle
        Piece[] pieces = bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof Cow &&
                spooker.getDistance(pieces[ii]) == 1) {
                Effect effect = ((Cow)pieces[ii]).spook(bangobj, spooker);
                if (effect != null) {
                    _bangmgr.deployEffect(spooker.owner, effect);
                }
            }
        }
    }
}
