//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Teleporter;

import static com.threerings.bang.Log.log;

/**
 * An effect deployed when a unit steps on a teleporter.
 */
public class TeleportEffect extends Effect
{
    /** The effect identifier. */
    public static final String TELEPORTED = "teleported";

    /** The id of the teleported piece. */
    public int pieceId;

    /** The coordinates to which the piece will be moved. */
    public short[] dest;

    /** The id of the source teleporter. */
    public transient int sourceId;

    public TeleportEffect ()
    {
    }

    public TeleportEffect (Teleporter teleporter, Piece piece)
    {
        sourceId = teleporter.pieceId;
        pieceId = piece.pieceId;
    }

    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Teleporter source = (Teleporter)bangobj.pieces.get(sourceId);
        if (source == null) {
            log.warning("Missing source teleporter for teleport effect [id=" +
                sourceId + "].");
            return;
        }

        // select a random destination teleporter
        Teleporter[] group = source.getGroup(bangobj);
        ArrayList<Teleporter> dests = new ArrayList<Teleporter>();
        for (Teleporter tport : group) {
            if (!tport.equals(source) &&
                bangobj.board.isOccupiable(tport.x, tport.y)) {
                dests.add(tport);
            }
        }
        Collections.shuffle(dests);
        Point spot = null;
        for (Teleporter dest : dests) {
            spot = bangobj.board.getOccupiableSpot(dest.x, dest.y, 2);
            if (spot != null) {
                break;
            }
        }
        if (spot != null) {
            dest = new short[] { (short)spot.x, (short)spot.y };
        }
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return dest != null;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing teleported piece for teleport effect " +
                        "[id=" + pieceId + "].");
            return false;
        }

        // move the piece and report the effect
        bangobj.board.clearShadow(piece);
        piece.position(dest[0], dest[1]);
        bangobj.board.shadowPiece(piece);
        reportEffect(obs, piece, TELEPORTED);
        return true;
    }
}
