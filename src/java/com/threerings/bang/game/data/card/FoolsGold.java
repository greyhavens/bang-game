//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Bonus;

/**
 * Places a "fool's nugget" on the board that looks like a gold nugget, but
 * isn't one.
 */
public class FoolsGold extends AreaCard
{
    @Override // documentation inherited
    public String getType ()
    {
        return "fools_gold";
    }
    
    @Override // documentation inherited
    public boolean isPlayable (BangObject bangobj)
    {
        return bangobj.scenarioId.equals(ScenarioCodes.CLAIM_JUMPING) ||
            bangobj.scenarioId.equals(ScenarioCodes.GOLD_RUSH);
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
        return 35;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        Bonus bonus = Bonus.createBonus(
            BonusConfig.getConfig("frontier_town/fools_nugget"));
        bonus.position(coords[0], coords[1]);
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
