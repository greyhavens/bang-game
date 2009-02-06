//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.ClearWreckageEffect;
import com.threerings.bang.game.data.effect.Effect;

/**
 * Clears all wreckage from the board.
 */
public class TumbleweedWind extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "tumbleweed_wind";
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }
    
    @Override // documentation inherited
    public PlacementMode getPlacementMode ()
    {
        return PlacementMode.VS_BOARD;
    }
    
    @Override // documentation inherited
    public boolean isValid (BangObject bangobj)
    {
        return true;
    }
    
    @Override // documentation inherited
    public int getWeight ()
    {
        return 40;
    }

    @Override // documentation inherited
    public Badge.Type getQualifier ()
    {
        return Badge.Type.TREES_SAVED_2;
    }

    @Override // documenataion inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        return new ClearWreckageEffect();
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 20;
    }
}
