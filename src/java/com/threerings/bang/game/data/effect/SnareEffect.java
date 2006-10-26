//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.client.effect.IconInfluenceViz;
import com.threerings.bang.game.client.effect.InfluenceViz;

/**
 * Stops a unit from moving for some ticks.
 */
public class SnareEffect extends SetHindranceEffect
{
    /** Fired off on the snare when activated. */
    public static final String ACTIVATED_SNARE = "indian_post/snare";
    
    /** Fired off on the snared unit. */
    public static final String ENSNARED = "indian_post/ensnared";
    
    @Override // documentation inherited
    protected Hindrance createHindrance (final Unit target)
    {
        return new Hindrance() {
            public String getName () {
                return "snare";
            }
            public InfluenceViz createViz (boolean high) {
                return new IconInfluenceViz("snare");
            }
            public int adjustMoveDistance (int moveDistance) {
                return 0;
            }
            public boolean isVisible () {
                return true;
            }
            protected int duration ()
            {
                return 8;
            }
        };
    }

    @Override // documentation inherited
    protected String getActivatedEffect ()
    {
        return ACTIVATED_SNARE;
    }

    @Override // documentation inherited
    protected String getEffectName()
    {
        return ENSNARED;
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx || pidx == -1) {
            return null;
        }
        return MessageBundle.compose("m.effect_snare", piece.getName());
    }
}
