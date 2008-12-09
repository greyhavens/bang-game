//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.ArrayList;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Sets the high noon hindrance on all units.
 */
public class HighNoonEffect extends Effect
{
    /** The value of {@link BangObject#boardEffect} when the high noon effect
     * is active. */
    public static final String HIGH_NOON = "frontier_town/high_noon";

    /** Generated when the effect expires. */
    public static class ExpireEffect extends ExpireHindranceEffect
    {
        @Override // documentation inherited
        public boolean apply (BangObject bangobj, Observer obs)
        {
            // clear the effect the first time applied
            if (bangobj.boardEffect != null) {
                bangobj.globalHindrance = null;
                affectBoard(bangobj, null, true, obs);
            }
            return super.apply(bangobj, obs);
        }
    }

    /** The ids of the units affected. */
    public int[] pieceIds;

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return pieceIds;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        if (HIGH_NOON.equals(bangobj.boardEffect)) {
            return;
        }
        ArrayList<Integer> pieceIds = new ArrayList<Integer>();
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Unit && piece.isAlive()) {
                pieceIds.add(piece.pieceId);
            }
        }
        this.pieceIds = CollectionUtil.toIntArray(pieceIds);
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return pieceIds != null;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Hindrance hindrance = new Hindrance() {
            public String getName () {
                return null;
            }
            public ExpireInfluenceEffect createExpireEffect () {
                return new ExpireEffect();
            }
            public int adjustMoveDistance (int moveDistance) {
                return moveDistance / 2;
            }
            protected int duration () {
                return EFFECT_DURATION;
            }
        };
        for (int pieceId : pieceIds) {
            Unit unit = (Unit)bangobj.pieces.get(pieceId);
            if (unit != null) {
                unit.setHindrance(hindrance);
                reportEffect(obs, unit, UPDATED);
            } else {
                log.warning("Missing piece for high noon effect", "id", pieceId);
            }
        }
        hindrance.init(bangobj.tick);
        bangobj.globalHindrance = hindrance;

        affectBoard(bangobj, HIGH_NOON, true, obs);

        return true;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        return "m.effect_high_noon";
    }

    /** The duration of the effect in ticks. */
    protected static final int EFFECT_DURATION = 8;
}
