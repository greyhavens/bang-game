//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.effect.HeroInfluenceViz;
import com.threerings.bang.game.client.effect.InfluenceViz;

/**
 * Provides bonuses to a hero based on its level.
 */
public class HeroInfluence extends Influence
{
    public HeroInfluence (byte level)
    {
        _level = level;
    }

    public HeroInfluence ()
    {
    }

    // documentation inherited
    public String getName ()
    {
        return "hero";
    }

    @Override // documentation inherited
    public InfluenceViz createViz (boolean high) {
        if (_viz == null) {
            _viz = new HeroInfluenceViz(_level);
        }
        return _viz;
    }

    /**
     * Sets the hero level for the influence.
     */
    public void setLevel (byte level)
    {
        _level = level;
        if (_viz != null) {
            _viz.setLevel(_level);
        }
    }

    @Override // documentation inherited
    public int adjustTicksPerMove (int ticksPerMove)
    {
        return ticksPerMove - (_level > 5 ? 1 : 0);
    }

    @Override // documentation inherited
    public int adjustMoveDistance (int moveDistance)
    {
        return moveDistance + _level / 4;
    }

    @Override // documentation inherited
    public int adjustMaxFireDistance (int fireDistance)
    {
        return fireDistance + (_level > 6 ? 1 : 0);
    }

    @Override // documentation inherited
    public int adjustAttack (Piece target, int damage)
    {
        return (damage + 3 * _level);
    }

    @Override // documentation inherited
    public int adjustDefend (Piece shooter, int damage)
    {
        return Math.max(0, (damage  - 3 * _level));
    }

    @Override // documentation inherited
    public int adjustProxDefend (Piece shooter, int damage)
    {
        return (_level > 8 ? 0 : damage);
    }

    @Override // documentation inherited
    public boolean adjustCorporeality (boolean corporeal)
    {
        return (_level > 4 ? false : corporeal);
    }

    @Override // documentation inherited
    public boolean showClientAdjust ()
    {
        return true;
    }

    @Override // documentation inherited
    public boolean didAdjustAttack ()
    {
        return _level > 0;
    }

    @Override // documentation inherited
    public boolean didAdjustDefend ()
    {
        return _level > 0;
    }

    @Override // documentation inherited
    public boolean resetTicksOnKill ()
    {
        return false;
    }

    protected byte _level;
    protected HeroInfluenceViz _viz;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Influence.class, "holding", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Influence.class, "_startTick", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.HeroInfluence.class, "_level", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.HeroInfluence.class, "_viz", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Influence.class, "holding", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Influence.class, "_startTick", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.HeroInfluence.class, "_level", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.HeroInfluence.class, "_viz", this, out);
    }
}
