//
// $Id$

package com.threerings.bang.data.effect;

import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.data.piece.Unit;

/**
 * An effect that replaces all dead units with 60% health windup
 * gunslingers owned by the activating player.
 */
public class SaintElmosEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String ELMOED = "elmoed";

    public int owner;
    public int[] pieceIds;
    public Piece[] newPieces;

    public SaintElmosEffect (int owner)
    {
        this.owner = owner;
    }

    public SaintElmosEffect ()
    {
    }

    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // roll through and note all dead pieces
        ArrayIntSet pieces = new ArrayIntSet();
        for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
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
            newPieces[ii].assignPieceId();
            newPieces[ii].init();
            newPieces[ii].owner = owner;
            newPieces[ii].damage = 60;
        }
    }

    public void apply (BangObject bangobj, Observer obs)
    {
        // remove the old pieces and add new windup gun slingers instead
        for (int ii = 0; ii < pieceIds.length; ii++) {
            Piece p = (Piece)bangobj.pieces.get(pieceIds[ii]);
            if (p == null) {
                continue;
            }
            bangobj.removePieceDirect(p);
            reportEffect(obs, p, ELMOED);
            reportRemoval(obs, p);

            newPieces[ii].position(p.x, p.y);
            bangobj.addPieceDirect(newPieces[ii]);
            reportAddition(obs, newPieces[ii]);
        }

        // the balance of power has shifted, recompute our stats
        bangobj.updateStats();
    }
}
