//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that causes the piece in question to hustle up and move in one
 * fewer ticks than normal until it is killed and respawned.
 */
public class HustleEffect extends BonusEffect
{
    /** The influence we have on units. */
    public static class HustleInfluence extends Influence
    {
        @Override // documentation inherited
        public int adjustTicksPerMove (int ticksPerMove) {
            return ticksPerMove-1;
        }
    }

    /** The identifier for the type of effect that we produce. */
    public static final String GIDDY_UP = "bonuses/hustle/activate";

    /** The piece we will affect. */
    public int pieceId;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // nothing doing
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit == null) {
            return;
        }

        unit.setInfluence(new HustleInfluence(), bangobj.tick);
        reportEffect(obs, unit, GIDDY_UP);
    }
}
