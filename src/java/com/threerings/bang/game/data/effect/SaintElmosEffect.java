//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that replaces all dead units with 60% health windup
 * gunslingers owned by the activating player.
 */
public class SaintElmosEffect extends BonusEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String ELMOED = "boom_town/saint_elmo";

    public int owner;
    public int[] pieceIds;
    public Piece[] newPieces;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        super.init(piece);
        owner = piece.owner;
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

        // roll through and note all dead pieces
        ArrayIntSet pieces = new ArrayIntSet();
        for (Piece p : bangobj.pieces) {
            if (!p.isAlive() && p.owner >= 0 &&
                // make sure we don't try to turn a dirigible over a
                // building or water into a windup gunman
                bangobj.board.isGroundOccupiable(p.x, p.y)) {
                pieces.add(p.pieceId);
            }
        }
        pieceIds = pieces.toIntArray();
        newPieces = new Piece[pieceIds.length];
        for (int ii = 0; ii < newPieces.length; ii++) {
            newPieces[ii] = Unit.getUnit("windupslinger");
            newPieces[ii].assignPieceId(bangobj);
            newPieces[ii].init();
            newPieces[ii].setOwner(bangobj, owner);
            newPieces[ii].damage = 60;
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        // remove the old pieces and add new windup gun slingers instead
        for (int ii = 0; ii < pieceIds.length; ii++) {
            Piece p = bangobj.pieces.get(pieceIds[ii]);
            if (p == null) {
                continue;
            }
            reportEffect(obs, p, ELMOED);
            removeAndReport(bangobj, p, obs);

            newPieces[ii].position(p.x, p.y);
            addAndReport(bangobj, newPieces[ii], obs);
        }

        // the balance of power has shifted, recompute our metrics
        bangobj.updateData();

        return true;
    }
}
