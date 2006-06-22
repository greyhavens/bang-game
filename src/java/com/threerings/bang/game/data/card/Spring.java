//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Bonus;

/**
 * Allows players to put a barely-visible spring on the board that
 * sends any unit that steps on it back to the location the unit came from.
 */
public class Spring extends AreaCard
{
    @Override // documentation inherited
    public String getType ()
    {
        return "spring";
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
        return 25;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        Bonus bonus = Bonus.createBonus(
            BonusConfig.getConfig("frontier_town/spring"));
        bonus.position(coords[0], coords[1]);
        return new AddPieceEffect(bonus);
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 0;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
