//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.data.TerrainConfig;

import com.threerings.bang.game.client.effect.InfluenceViz;
import com.threerings.bang.game.client.effect.NoncorporealViz;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Causes a unit to become noncorporeal.
 */
public class NoncorporealEffect extends SetInfluenceEffect
{
    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        return new Influence() {
            public String getName () {
                return "spirit_walk";
            }
            public InfluenceViz createViz (boolean high) {
                return new NoncorporealViz();
            }
            public int adjustTraversalCost (
                    TerrainConfig terrain, int traversalCost) {
                return BangBoard.BASE_TRAVERSAL;
            }
            public boolean adjustCorporeality (boolean corporeal) {
                return false;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "indian_post/spirit_walk";
    }
}
