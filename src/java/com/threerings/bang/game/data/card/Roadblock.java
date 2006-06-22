//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Wreckage;

/**
 * Allows players to put an impassable road block on the board that
 * expires after some number of ticks.
 */
public class Roadblock extends AreaCard
{
    @Override // documentation inherited
    public String getType ()
    {
        return "roadblock";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 0;
    }

    @Override // documentation inherited
    public boolean isValidLocation (BangObject bangobj, int tx, int ty)
    {
        return (bangobj.board.isOccupiable(tx, ty));
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 50;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        Wreckage wreckage = new Wreckage();
        wreckage.position(coords[0], coords[1]);
        return new AddPieceEffect(wreckage);
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 100;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
