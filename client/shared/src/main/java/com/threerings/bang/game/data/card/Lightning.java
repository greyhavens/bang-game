//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.LightningEffect;
import com.threerings.bang.game.data.piece.Piece;

/**
 * A card that shoots lightning at a unit.
 */
public class Lightning extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "lightning";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target.isTargetable() && target.isAlive());
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 45;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 30;
    }

    @Override // documentation inherited
    public Badge.Type getQualifier ()
    {
        return Badge.Type.STORM_CALLER_USER;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        LightningEffect effect = new LightningEffect();
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
