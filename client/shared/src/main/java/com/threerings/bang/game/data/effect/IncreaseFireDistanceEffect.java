//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Increases the max fire distance of a unit.
 */
public class IncreaseFireDistanceEffect extends SetInfluenceEffect
{
    /** The amount to increase the max fire distance. */
    public int fireDistanceIncrease = 1;

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return _unit != null && super.isApplicable() && 
            _unit.getConfig().gunUser && _unit.getConfig().maxFireDistance > 1;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        _unit = (Unit)bangobj.pieces.get(pieceId);
    }

    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        return new Influence() {
            public String getName() {
                return "eagle_eye";
            }

            public int adjustMaxFireDistance (int fireDistance)
            {
                return fireDistance + fireDistanceIncrease;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "indian_post/eagle_eye";
    }

    /** Reference to the unit we'll be influencing. */
    protected transient Unit _unit;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.SetInfluenceEffect.class, "influenceType", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.IncreaseFireDistanceEffect.class, "fireDistanceIncrease", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.SetInfluenceEffect.class, "influenceType", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.IncreaseFireDistanceEffect.class, "fireDistanceIncrease", this, out);
    }
}
