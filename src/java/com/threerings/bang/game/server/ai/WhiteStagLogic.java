//
// $Id$

package com.threerings.bang.game.server.ai;

import java.util.ArrayList;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Handles controlling the White Stag.
 */
public class WhiteStagLogic extends PieceLogic
{
    @Override // documentation inherited
    public void tick (Piece[] pieces, short tick)
    {
        _shooters.addAll(_bangmgr.getShooters());
        super.tick(pieces, tick);
    }

    @Override // documentation inherited
    protected void moveUnit (
       Piece[] pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        // try to attack someone who attacked since our last move
        ArrayList<Piece> targets = new ArrayList<Piece>();
        for (Piece p : pieces) {
            if (p.isTargetable() && attacks.contains(p.x, p.y) &&
                    _shooters.contains(p.pieceId) && 
                    unit.validTarget(_bangobj, p, true)) {
                targets.add(p);
            }
        }
        _shooters.clear();
        if (!targets.isEmpty() && executeOrder(unit, Short.MAX_VALUE, 0, 
                    targets.get(RandomUtil.getInt(targets.size())))) {
            return;
        }
        
        // otherwise just move
        if (moves.size() == 0) {
            return;
        }
        int midx = RandomUtil.getInt(moves.size());
        executeOrder(unit, moves.getX(midx), moves.getY(midx), null);
    }

    protected ArrayIntSet _shooters = new ArrayIntSet();
}
