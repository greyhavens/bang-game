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
public class IncreaseMaxFireEffect extends SetInfluenceEffect
{
    /** The amount to increase the max fire. */
    public int maxFireIncrease = 1;

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
                return fireDistance + maxFireIncrease;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "eagle_eye";
    }

    /** Reference to the unit we'll be influencing. */
    protected transient Unit _unit;
}
