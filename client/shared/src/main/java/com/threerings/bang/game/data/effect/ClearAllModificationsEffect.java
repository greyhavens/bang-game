//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Clears all the modifications.
 */
public class ClearAllModificationsEffect extends GlobalEffect
{
    /** The rain effect applied to the board. */
    public static final String RAIN = "indian_post/rain";
    
    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        affectBoard(bangobj, RAIN, false, obs);
        return super.apply(bangobj, obs);
    }
    
    @Override // documentation inherited
    protected boolean validPiece (Piece piece)
    {
        return (piece instanceof Unit && (((Unit)piece).getMainInfluence() != null ||
                    ((Unit)piece).getHindrance() != null));
    }

    @Override // documentation inherited
    protected Effect getEffect (Piece piece)
    {
        ClearModificationsEffect effect = new ClearModificationsEffect();
        effect.init(piece);
        return effect;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        return "m.effect_clear_all_modifications";
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.GlobalEffect.class, "effects", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.GlobalEffect.class, "effects", this, out);
    }
}
