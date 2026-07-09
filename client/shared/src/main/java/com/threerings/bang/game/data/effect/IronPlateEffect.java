//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.client.effect.InfluenceViz;
import com.threerings.bang.game.client.effect.IronPlateViz;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that causes the piece in question to become invincible for seven
 * ticks.
 */
public class IronPlateEffect extends SetInfluenceEffect
{
    /** This is a free iron plate, so everyone gets to see it. */
    public boolean freebie;

    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        final boolean isFreebie = freebie;
        return new Influence() {
            public String getName () {
                return "iron_plate";
            }
            public InfluenceViz createViz (boolean high) {
                return new IronPlateViz();
            }
            public int adjustDefend (Piece shooter, int damage) {
                return 0;
            }
            public int adjustProxDefend (Piece shooter, int damage) {
                return 0;
            }
            public boolean didAdjustDefend () {
                return true;
            }
            public boolean hidden () {
                return !isFreebie;
            }
            public boolean showClientAdjust () {
                return isFreebie;
            }
            protected int duration () {
                return 7;
            }
        };
    }

    @Override // documentation inherited
    protected String getActivatedEffect ()
    {
        return null; // handled by the influence viz
    }
    
    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "frontier_town/iron_plate";
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.SetInfluenceEffect.class, "influenceType", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.IronPlateEffect.class, "freebie", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.SetInfluenceEffect.class, "influenceType", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.IronPlateEffect.class, "freebie", this, out);
    }
}
