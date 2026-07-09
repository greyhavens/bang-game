//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.client.effect.InfluenceViz;
import com.threerings.bang.game.client.effect.ParticleInfluenceViz;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that causes the piece in question to become "lucky" which gives it
 * a chance of doing 2x damage on future attacks.
 */
public class LadyLuckEffect extends SetInfluenceEffect
{
    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        return new Influence() {
            public String getName () {
                return "lady_luck";
            }
            public InfluenceViz createViz (boolean high) {
                return new ParticleInfluenceViz("frontier_town/lucky");
            }
            public int adjustAttack (Piece target, int damage) {
                _didAdjustAttack = RandomUtil.getInt(100) >= 50;
                return _didAdjustAttack ? 2 * damage : damage;
            }
            public boolean didAdjustAttack () {
                return _didAdjustAttack;
            }

            protected boolean _didAdjustAttack = false;
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "frontier_town/lady_luck";
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
