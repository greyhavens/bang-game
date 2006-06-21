//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Bonus;

/**
 * The Joker: players put it on the board, where it looks like a card bonus.
 * When a unit tries to claim it, it explodes in a burst of the placing team's
 * color, causing damage and removing influences.
 */
public class Joker extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "joker";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 0;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 25;
    }

    @Override // documentation inherited
    public Effect activate (int x, int y)
    {
        Bonus bonus = Bonus.createBonus(
            BonusConfig.getConfig("frontier_town/joker"));
        bonus.position(x, y);
        return new AddPieceEffect(bonus);
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 150;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
