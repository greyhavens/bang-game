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
    public boolean isPlayable (BangObject bangobj, String townId)
    {
        if (!super.isPlayable(bangobj, townId)) {
            return false;
        }

        // make sure the board contains train tracks with at least one junction
        for (Track track : bangobj.getTracks().values()) {
            if (track.getAdjacent(bangobj).length > 2) {
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
        // make sure an engine can reach the destination
        Track track = bangobj.getTracks().get(Piece.coord(tx, ty));
        if (track == null) {
            return false;
        }
        for (Piece piece : bangobj.pieces) {
            if (!(piece instanceof Train)) {
                continue;
            }
            Train train = (Train)piece;
            if (train.type == Train.ENGINE && train.findPath(bangobj, track) != null) {
                return true;
            }
        }
        return false;
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
        return 20;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        return new ControlTrainEffect(owner, coords[0], coords[1]);
    }
}
