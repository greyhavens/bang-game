//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ControlTrainEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Train;

/**
 * Allows the player to choose a destination for the train.
 */
public class Engineer extends AreaCard
{
    @Override // documentation inherited
    public String getType ()
    {
        return "engineer";
    }

    @Override // documentation inherited
    public boolean isPlayable (BangObject bangobj)
    {
        if (!super.isPlayable(bangobj)) {
            return false;
        }

        // make sure the board contains train tracks
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Track) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 0;
    }

    @Override // documentation inherited
    public boolean isValidLocation (BangObject bangobj, int tx, int ty)
    {
        // find the engine and piece of track and make sure the engine
        // can reach the destination
        Train engine = null;
        Track track = null;
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Train &&
                ((Train)piece).type == Train.ENGINE) {
                engine = (Train)piece;
            } else if (piece instanceof Track && piece.intersects(tx, ty)) {
                track = (Track)piece;
            }
        }
        return (engine == null || track == null) ?
            false : (engine.findPath(bangobj, track) != null);
    }

    @Override // documentation inherited
    public boolean shouldShowVisualization (int pidx)
    {
        return pidx == owner;
    }
    
    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 60;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 60;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        return new ControlTrainEffect(owner, coords[0], coords[1]);
    }
}
