//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayUtil;

import com.threerings.bang.game.data.BangObject;

/**
 * Represents a unit's picking up or dropping a fool's nugget.
 */
public class FoolsNuggetEffect extends NuggetEffect
{
    /** The bonus type for a nugget of fool's gold. */
    public static final String FOOLS_NUGGET_BONUS =
        "frontier_town/fools_nugget";
    
    /** The identifier for the fool's gold nugget rejection effect. */
    public static final String FOOLS_NUGGET_REJECTED =
        "bonuses/frontier_town/fools_nugget/rejected";
        
    public FoolsNuggetEffect ()
    {
        type = FOOLS_NUGGET_BONUS;
    }
    
    @Override // documentation inherited
    protected void applyToClaim (BangObject bangobj, Observer obs)
    {
        reportEffect(obs, bangobj.pieces.get(claimId), FOOLS_NUGGET_REJECTED);
    }
}
