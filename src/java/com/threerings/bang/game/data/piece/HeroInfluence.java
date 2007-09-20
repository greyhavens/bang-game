//
// $Id$

package com.threerings.bang.game.data.piece;

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

    /**
     * Sets the hero level for the influence.
     */
    public void setLevel (byte level)
    {
        _level = level;
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
        return (int)(damage + 3 * _level);
    }

    @Override // documentation inherited
    public int adjustDefend (Piece shooter, int damage)
    {
        return (int)(damage  - 3 * _level);
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
    public boolean removeWhenDead (boolean remove)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean removeOnKill ()
    {
        return false;
    }

    protected byte _level;
}
