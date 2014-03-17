//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.Collections;
import java.util.List;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Converts some number of units from the other players to the activating
 * players control.
 */
public class DefectEffect extends BonusEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String DEFECTED = "frontier_town/defect";

    public int activator;

    public int[] pieceIds = new int[0];

    @Override // documentation inherited
    public void init (Piece piece)
    {
        activator = piece.owner;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return pieceIds;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);

        // determine which of our opponents is the strongest and steal one
        // of their pieces
        int maxpower = Integer.MIN_VALUE, pidx = -1;
        for (int ii = 0; ii < bangobj.pdata.length; ii++) {
            if (bangobj.pdata[ii].power > maxpower) {
                pidx = ii;
                maxpower = bangobj.pdata[ii].power;
            }
        }
        if (pidx == -1) {
            log.warning("Failed to find player for defect", "activator", activator,
                        "pdata", StringUtil.toString(bangobj.pdata));
            return;
        }

        // if the activator is the most powerful player, do nothing
        if (pidx == activator) {
            return;
        }

        // now steal a random non-bigshot, non-nuggeted unit from this player
        List<Piece> pieces = bangobj.getPieceArray();
        Collections.shuffle(pieces);
        for (Piece p : pieces) {
            if (p.owner != pidx || !(p instanceof Unit)) {
                continue;
            }
            Unit unit = (Unit)p;
            if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT ||
                NuggetEffect.NUGGET_BONUS.equals(unit.holding)) {
                continue;
            }
            pieceIds = new int[] { unit.pieceId };
            break;
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        // swipe away!
        int defected = 0;
        for (int ii = 0; ii < pieceIds.length; ii++) {
            Piece p = bangobj.pieces.get(pieceIds[ii]);
            if (p == null || !p.isAlive()) {
                continue;
            }
            p.setOwner(bangobj, activator);
            reportEffect(obs, p, DEFECTED);
            defected++;
        }
        if (defected > 0) {
            // the balance of power has shifted, recompute our metrics
            bangobj.updateData();
        }

        return true;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        if (pidx == -1) {
            return null;
        }
        String names = getPieceNames(bangobj, pidx, pieceIds);
        return (names == null) ? null :
            MessageBundle.compose("m.effect_defect", names,
                MessageBundle.taint(bangobj.players[activator]));
    }
}
