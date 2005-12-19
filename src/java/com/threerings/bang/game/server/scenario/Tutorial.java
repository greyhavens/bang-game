//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

/**
 * Handles the server side operation of tutorial scenarios.
 */
public class Tutorial extends Scenario
{
    @Override // documentation inherited
    public void init (BangManager bangman, BangObject bangobj,
        ArrayList<Piece> markers, PointSet bonusSpots, PieceSet purchases)
        throws InvocationException
    {
        super.init(bangman, bangobj, markers, bonusSpots, purchases);

        // TODO: obtain the tutorial identifier, load the tutorial config, set
        // up our various listeners
    }
}
