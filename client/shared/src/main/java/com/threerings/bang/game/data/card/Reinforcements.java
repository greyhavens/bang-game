//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.DuplicateEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to make a new Gunslinger that will not 
 * respawn and duplicates it's status from the selected unit.
 */
public class Reinforcements extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "reinforcements";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive() &&
                target.owner != -1);
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 10;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 0;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        DuplicateEffect effect = new DuplicateEffect();
        effect.pieceId = (Integer)target;
        return effect;
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
