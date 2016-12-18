//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.samskivert.util.Tuple;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Train;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;

/**
 * Handles the behavior of cattle.
 */
public class CattleDelegate extends ScenarioDelegate
{
    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        // find all the cows and their locations so that we can check
        // quickly if we can move any out of the way to free
        // blocked units
        _cows.clear();
        _clocs.clear();
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Cow) {
                _cows.add((Cow)piece);
                _clocs.add(piece.x, piece.y);
            }
        }

        // look for units completely penned in in order
        // to spook nearby cows
        _spooked.clear();
        for (Piece piece : bangobj.pieces) {
            if (!(piece instanceof Unit) || !piece.isAlive() ||
                !_clocs.containsAdjacent(piece.x, piece.y)) {
                continue;
            }
            _moves.clear();
            bangobj.board.computeMoves(piece, _moves, null);
            if (_moves.size() <= 1) {
                spookHerd(bangobj, piece);
            }
        }

        // fire off the spook effects in reverse order
        for (int ii = _spooked.size() - 1; ii >= 0; ii--) {
            Tuple<Cow, Piece> spooked = _spooked.get(ii);
            spook(bangobj, spooked.left, spooked.right, true);
        }

        return !_spooked.isEmpty();
    }

    @Override // documentation inherited
    public void pieceMoved (BangObject bangobj, Piece piece)
    {
        if (piece instanceof Unit) {
            checkSpookedCattle(bangobj, piece, 1);

        } else if (piece instanceof Train &&
                   ((Train)piece).type == Train.ENGINE) {
            checkSpookedCattle(bangobj, piece, 2);
        }
    }

    protected void checkSpookedCattle (
        BangObject bangobj, Piece spooker, int radius)
    {
        // check to see if this piece spooked any cattle
        for (Piece piece : bangobj.getPieceArray()) {
            if (piece instanceof Cow && spooker.getDistance(piece) <= radius) {
                spook(bangobj, (Cow)piece, spooker, false);
            }
        }
    }

    protected void spookHerd (BangObject bangobj, Piece spooker)
    {
        // add the cows connected to the spooker to a list in order of
        // increasing distance using a breadth-first search
        List<Piece> fringe = Lists.newArrayList();
        fringe.add(spooker);
        while (!fringe.isEmpty()) {
            List<Piece> nfringe = Lists.newArrayList();
            for (Piece piece : fringe) {
                for (Iterator<Cow> it = _cows.iterator(); it.hasNext(); ) {
                    Cow cow = it.next();
                    if (piece.getDistance(cow) == 1) {
                        it.remove();
                        _clocs.remove(cow.x, cow.y); // only spook cows once
                        _spooked.add(new Tuple<Cow, Piece>(cow, spooker));
                        nfringe.add(cow);
                    }
                }
            }
            fringe = nfringe;
        };
    }

    protected void spook (
        BangObject bangobj, Cow cow, Piece spooker, boolean herd)
    {
        Effect effect = cow.spook(bangobj, spooker, herd);
        if (effect != null) {
            _bangmgr.queueDeployEffect(spooker.owner, effect, false);
        }
    }

    protected PointSet _moves = new PointSet(), _clocs = new PointSet();
    protected List<Cow> _cows = Lists.newArrayList();
    protected List<Tuple<Cow, Piece>> _spooked = Lists.newArrayList();
}
