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
