//
// $Id$

package com.threerings.bang.game.data.piece;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;

/**
 * Handles the special capabilities of the Codger unit.
 */
public class Codger extends Unit
{
    public static class CodgeredEffect extends Effect
    {
        public static final String CODGERED = "codgered";
        public int pieceId;
        public short newLastActed;

        public CodgeredEffect () {
        }

        public CodgeredEffect (Piece target) {
            pieceId = target.pieceId;
            newLastActed = (short)(target.lastActed+1);
        }

        public void prepare (BangObject bangobj, IntIntMap dammap) {
        }

        public void apply (BangObject bangobj, Observer observer) {
            Piece piece = (Piece)bangobj.pieces.get(pieceId);
            if (piece != null) {
                piece.lastActed = newLastActed;
                reportEffect(observer, piece, CODGERED);
            }
        }
    }

    @Override // documentation inherited
    public Effect[] collateralDamage (
        BangObject bangobj, Piece target, int damage)
    {
        // the codger adds a tick to the unit he fires upon
        if (target.lastActed < bangobj.tick) {
            return new Effect[] { new CodgeredEffect(target) };
        }
        return null;
    }
}
