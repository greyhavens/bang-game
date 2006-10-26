//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.AddWhiteStagEffect;

/**
 * A card that adds a compute controlled white stag to the board.
 */
public class WhiteStag extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "white_stag";
    }

    @Override // documentation inherited
    public PlacementMode getPlacementMode ()
    {
        return PlacementMode.VS_AREA;
    }

    @Override // documentation inherited
    public boolean isValidLocation (BangObject bangobj, int tx, int ty)
    {
        return (bangobj.board.isOccupiable(tx, ty));
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 0; // DISABLED: 15;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        return new AddWhiteStagEffect(coords[0], coords[1]);
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 0;
    }
}
