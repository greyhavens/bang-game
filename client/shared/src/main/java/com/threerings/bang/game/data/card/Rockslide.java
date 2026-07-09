//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.RockslideEffect;

/**
 * A card that sends a rockslide that injures units in its path.
 */
public class Rockslide extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "rockslide";
    }

    @Override // documentation inherited
    public PlacementMode getPlacementMode ()
    {
        return PlacementMode.VS_AREA;
    }

    @Override // documentation inherited
    public boolean isValidLocation (BangObject bangobj, int tx, int ty)
    {
        return (bangobj.board.getTerrainSlope(tx, ty) >= 0);
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 0; // DISABLED: 35;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 40;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        return new RockslideEffect(coords[0], coords[1], owner);
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.card.Card.class, "cardId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.card.Card.class, "owner", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.card.Card.class, "found", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.card.Card.class, "cardId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.card.Card.class, "owner", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.card.Card.class, "found", this, out);
    }
}
