//
// $Id$

package com.threerings.bang.game.server.ai;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;

/**
 * Picks random big shot and units, moves randomly, and fires randomly.
 */
public class RandomLogic extends AILogic
{
    // documentation inherited
    public String getBigShotType ()
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.BIGSHOT);
        return RandomUtil.pickRandom(configs).type;
    }

    // documentation inherited
    public String[] getUnitTypes (int count)
    {
        String[] types = new String[count];
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.NORMAL);
        ArrayUtil.shuffle(configs);
        for (int ii = 0; ii < count; ii++) {
            types[ii] = configs[ii].type;
        }
        return types;
    }
    
    // documentation inherited
    protected void moveUnit (
        Piece[] pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        // if we can attack someone, do that
        Piece target = null;
        for (int tt = 0; tt < pieces.length; tt++) {
            Piece p = pieces[tt];
            if (p instanceof Unit && attacks.contains(p.x, p.y) &&
                unit.validTarget(_bangobj, p, false)) {
                target = p;
                break;
            }
        }
        if (target != null && executeOrder(unit, Short.MAX_VALUE, 0, target)) {
            return;
        }

        // otherwise just move
        if (moves.size() == 0) {
            return;
        }
        int midx = RandomUtil.getInt(moves.size());
        executeOrder(unit, moves.getX(midx), moves.getY(midx), null);
    }
}
