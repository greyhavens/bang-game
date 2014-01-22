//
// $Id$

package com.threerings.bang.game.server.ai;

import java.util.List;

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
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId, UnitConfig.Rank.BIGSHOT);
        return RandomUtil.pickRandom(configs).type;
    }

    // documentation inherited
    public String[] getUnitTypes (int count)
    {
        String[] types = new String[count];
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId, UnitConfig.Rank.NORMAL);
        ArrayUtil.shuffle(configs);
        for (int ii = 0; ii < count; ii++) {
            types[ii] = configs[ii].type;
        }
        return types;
    }

    // documentation inherited
    protected void moveUnit (List<Piece> pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        moveUnitDegraded(pieces, unit, moves, attacks);
    }
}
