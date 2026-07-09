//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that gives a 30% attack bonus across the board to the piece in
 * question.
 */
public class PowerUpEffect extends SetInfluenceEffect
{
    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        return new Influence() {
            public String getName () {
                return "power_up";
            }
            public int adjustAttack (Piece target, int damage) {
                return Math.round(1.3f * damage);
            }
            public boolean didAdjustAttack () {
                return true;
            }
            public boolean showClientAdjust () {
                return true;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "frontier_town/power_up";
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.SetInfluenceEffect.class, "influenceType", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.SetInfluenceEffect.class, "influenceType", this, out);
    }
}
