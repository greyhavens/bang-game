//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.ArrayList;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.RandomUtil;

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
    
    /** The id of the destination teleporter. */
    public int destId = -1;
    
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
        
        // try to find an unoccupied destination
        Teleporter[] group = source.getGroup(bangobj);
        ArrayList<Teleporter> dests = new ArrayList<Teleporter>();
        for (Teleporter tport : group) {
            if (!tport.equals(source) &&
                bangobj.board.isOccupiable(tport.x, tport.y)) {
                dests.add(tport);
            }
        }
        if (!dests.isEmpty()) {
            destId = RandomUtil.pickRandom(dests).pieceId;
        }
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return destId > -1;
    }
    
    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing teleported piece for teleport effect [id=" +
                pieceId + "].");
            return false;
        }
        Teleporter dest = (Teleporter)bangobj.pieces.get(destId);
        if (dest == null) {
            log.warning("Missing dest teleporter for teleport effect [id=" +
                destId + "].");
            return false;
        }
        
        // move the piece and report the effect
        bangobj.board.clearShadow(piece);
        piece.position(dest.x, dest.y);
        piece.orientation = dest.orientation;
        bangobj.board.shadowPiece(piece);
        reportEffect(obs, piece, TELEPORTED);
        return true;
    }
}
