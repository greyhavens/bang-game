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

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        super.readObject(ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AddHeroEffect.class, "level", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        super.writeObject(out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AddHeroEffect.class, "level", this, out);
    }
}
