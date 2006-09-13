//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;
import java.util.HashSet;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Train;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;

/**
 * Handles the behavior of cattle.
 */
public class CattleDelegate extends ScenarioDelegate
{    
    @Override // documentation inherited
    public void tick (BangObject bangobj, short tick)
    {
        // look for units completely penned in in order
        // to spook nearby cows
        for (Piece piece : bangobj.pieces) {
            if (!(piece instanceof Unit)) {
                continue;
            }
            _moves.clear();
            bangobj.board.computeMoves(piece, _moves, null);
            if (_moves.size() <= 1) {
                spookHerd(bangobj, piece);
            }
        }
    }
    
    @Override // documentation inherited
    public void pieceMoved (BangObject bangobj, Piece piece)
    {
        if (piece instanceof Unit) {
            checkSpookedCattle(bangobj, (Unit)piece, 1);

        } else if (piece instanceof Train &&
                   ((Train)piece).type == Train.ENGINE) {
            checkSpookedCattle(bangobj, piece, 2);
        }
    }

    protected void checkSpookedCattle (
        BangObject bangobj, Piece spooker, int radius)
    {
        // check to see if this piece spooked any cattle
        Piece[] pieces = bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof Cow &&
                spooker.getDistance(pieces[ii]) <= radius) {
                spook(bangobj, (Cow)pieces[ii], spooker, false);
            }
        }
    }
    
    protected void spookHerd (BangObject bangobj, Piece spooker)
    {
        ArrayList<Piece> fringe = new ArrayList<Piece>(),
            spooked = new ArrayList<Piece>();
        
        // add the cows connected to the spooker to a list in order of
        // increasing distance using a breadth-first search
        fringe.add(spooker);
        while (!fringe.isEmpty()) {
            ArrayList<Piece> nfringe = new ArrayList<Piece>();
            for (Piece fp : fringe) {
                for (Piece p : bangobj.pieces) {
                    if (p instanceof Cow && fp.getDistance(p) == 1 &&
                        !spooked.contains(p)) {
                        spooked.add(p);
                        nfringe.add(p);
                    }
                }
            }
            fringe = nfringe;
        };
        
        // fire off the spook effects in reverse order
        for (int ii = spooked.size() - 1; ii >= 0; ii--) {
            spook(bangobj, (Cow)spooked.get(ii), spooker, true);
        }
    }
    
    protected void spook (
        BangObject bangobj, Cow cow, Piece spooker, boolean herd)
    {
        Effect effect = cow.spook(bangobj, spooker, herd);
        if (effect != null) {
            _bangmgr.deployEffect(spooker.owner, effect);
        }
    }
    
    protected PointSet _moves = new PointSet();
}
