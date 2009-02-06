//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.ClearAllModificationsEffect;
import com.threerings.bang.game.data.effect.Effect;

/**
 * Clears all unit modifications.
 */
public class Rain extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "rain";
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
        return 30;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 40;
    }

    @Override // documenataion inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        return new ClearAllModificationsEffect();
    }
}
