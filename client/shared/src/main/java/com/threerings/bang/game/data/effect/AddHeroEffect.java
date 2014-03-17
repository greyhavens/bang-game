//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.HeroInfluence;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Respawns a hero with the hero influence set.
 */
public class AddHeroEffect extends AddPieceEffect
{
    public byte level;

    public AddHeroEffect ()
    {
    }

    public AddHeroEffect (Unit unit, byte level)
    {
        super(unit, AddPieceEffect.RESPAWNED);
        this.level = level;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        ((Unit)piece).setInfluence(Unit.InfluenceType.SPECIAL, new HeroInfluence(level));
        super.apply(bangobj, obs);
        return true;
    }
}
