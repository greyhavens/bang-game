//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;

import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.server.BangServer;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.game.util.TutorialUtil;

/**
 * Handles the server side operation of tutorial scenarios.
 */
public class Tutorial extends Scenario
    implements PlaceManager.MessageHandler
{
    @Override // documentation inherited
    public void gameWillStart (BangObject bangobj, ArrayList<Piece> markers,
                               PointSet bonusSpots, PieceSet purchases)
        throws InvocationException
    {
        super.gameWillStart(bangobj, markers, bonusSpots, purchases);

        // register to receive various tutorial specific messages
        _bangmgr.registerMessageHandler(TutorialCodes.ACTION_PROCESSED, this);

        // load up the tutorial configuraton
        BangConfig bconfig = (BangConfig)_bangmgr.getConfig();
        _config = TutorialUtil.loadTutorial(
            BangServer.rsrcmgr, bconfig.scenarios[0]);

        // TODO: set up our game object listeners
    }

    @Override // documentation inherited
    public void startNextPhase (BangObject bangobj)
    {
        switch (bangobj.state) {
        case BangObject.POST_ROUND:
        case BangObject.PRE_GAME:
            // TODO: allow some tutorials to have select and buying phases
            // bangobj.setState(BangObject.SELECT_PHASE);
            // break;

        case BangObject.SELECT_PHASE:
            // bangobj.setState(BangObject.BUYING_PHASE);
            // break;

        case BangObject.BUYING_PHASE:
            _bangmgr.startGame();
            break;

        default:
            super.startNextPhase(bangobj);
            break;
        }
    }

    // documentation inherited from PlaceManager.MessageHandler
    public void handleEvent (MessageEvent event, PlaceManager pmgr)
    {
    }

    protected TutorialConfig _config;
}
