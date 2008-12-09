//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Train;

import static com.threerings.bang.Log.log;

/**
 * Directs the train towards a given location.
 */
public class ControlTrainEffect extends Effect
{
    /** Identifies the train being controlled. */
    public int group = -1;
    
    /** The index of the controlling player or -1 to clear the control. */
    public int player = -1;

    /** The desired train destination. */
    public transient int tx, ty;

    public ControlTrainEffect ()
    {
    }

    public ControlTrainEffect (int player, int tx, int ty)
    {
        this.player = player;
        this.tx = tx;
        this.ty = ty;
    }

    public ControlTrainEffect (int group)
    {
        this.group = group;
    }
    
    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return NO_PIECES;
    }

    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // no preparation required for releasing control
        if (player == -1) {
            return;
        }

        // find the piece of track and its group id
        Track dest = bangobj.getTracks().get(Piece.coord(tx, ty));
        if (dest == null) {
            log.warning("Missing destination track for control train effect", "player", player,
                        "tx", tx, "ty", ty);
            return;
        }
        
        // find the engine in the group
        Train engine = null;
        for (Piece piece : bangobj.pieces) {
            if (!(piece instanceof Train)) {
                continue;
            }
            Train train = (Train)piece;
            if (train.type == Train.ENGINE && train.group == dest.group) {
                engine = train;
                break;
            }
        }
        if (engine == null) {
            log.warning("Missing train engine for control train effect", "player", player);
            return;
        }
        
        // compute the path
        if ((engine.path = engine.findPath(bangobj, dest)) != null) {
            group = dest.group;
        } else {
            log.warning("Couldn't find path for control train effect", "engine", engine,
                        "dest", dest);
        }
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return (group >= 0);
    }
    
    // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        // update the owners for all connected train pieces
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Train && ((Train)piece).group == group) {
                piece.setOwner(bangobj, player);
                reportEffect(obs, piece, UPDATED);
            }
        }
        return true;
    }
}
