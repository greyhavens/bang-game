//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.RandomUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Duplicates a piece.
 */
public class DuplicateEffect extends BonusEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String DUPLICATED = "frontier_town/duplicate";

    /** Reported when a duplicate could not be placed for lack of room. */
    public static final String WASTED_DUP = "frontier_town/duplicated/wasted";

    /** The duplicate piece. */
    public Piece duplicate;

    /**
     * The constructor used when created by a duplicate bonus.
     */
    public DuplicateEffect ()
    {
    }

    /**
     * The constructor used when created by a duplicate card.
     *
     * @param type the type of unit to create instead of duplicating the unit
     * in question or null if we should use the unit's dup-type.
     */
    public DuplicateEffect (String type)
    {
        _type = type;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        DuplicateEffect effect = (DuplicateEffect)super.clone();
        effect.duplicate = (Piece)duplicate.clone();
        return effect;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId, duplicate.pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit == null) {
            return;
        }

        // find a place to put our new unit
        Point spot = bangobj.board.getOccupiableSpot(unit.x, unit.y, 2);
        if (spot == null) {
            log.info("Dropped duplicate effect. No spots", "unit", unit);
            return;
        }

        super.prepare(bangobj, dammap);

        // if no unit was specified, pick one randomly based on their
        // duplication probability
        if (_type == null) {
            UnitConfig[] configs = UnitConfig.getTownUnits(
                    bangobj.townId, UnitConfig.Rank.NORMAL);
            int[] ranks = new int[configs.length];
            for (int ii = 0; ii < ranks.length; ii++) {
                ranks[ii] = configs[ii].dupeProb;
            }
            int idx = RandomUtil.getWeightedIndex(ranks);
            _type = configs[idx].type;
        }

        // position our new unit
        duplicate = unit.duplicate(bangobj, _type);
        duplicate.position(spot.x, spot.y);
        if (duplicate instanceof Unit) {
            ((Unit)duplicate).originalOwner = -1;
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing target for dup effect", "pid", pieceId);
            return false;
        }

        if (duplicate == null) {
            // report wastage if we were unable to place the new piece
            reportEffect(obs, piece, WASTED_DUP);
        } else {
            // add the new piece, informing the observer
            addAndReport(bangobj, duplicate, obs);
            // inform the observer of our duplication
            reportEffect(obs, duplicate, DUPLICATED);
        }

        return true;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx || pidx == -1) {
            return null;
        }
        return MessageBundle.compose("m.effect_duplicate", piece.getName());
    }

    protected transient String _type;
}
