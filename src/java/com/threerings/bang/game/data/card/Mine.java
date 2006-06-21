//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Bonus;

/**
 * Allows players to put a barely-visible mine on the board that
 * explodes on contact.
 */
public class Mine extends AreaCard
{
    @Override // documentation inherited
    public String getType ()
    {
        return "mine";
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
    public Effect activate (int x, int y)
    {
        Bonus bonus = Bonus.createBonus(
            BonusConfig.getConfig("frontier_town/mine"));
        bonus.position(x, y);
        return new AddPieceEffect(bonus);
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
