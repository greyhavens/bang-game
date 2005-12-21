//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.server.BangServer;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
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

        // set up our game object listeners
        _bangobj = bangobj;
        _bangobj.addListener(_acl);

        // start by executing the zeroth action
        _bangobj.setActionId(0);
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        if (super.tick(bangobj, tick)) {
            return true;
        }
        return _nextActionId >= _config.getActionCount();
    }

    // documentation inherited from PlaceManager.MessageHandler
    public void handleEvent (MessageEvent event, PlaceManager pmgr)
    {
        String name = event.getName();
        if (name.equals(TutorialCodes.ACTION_PROCESSED)) {
            _nextActionId = (Integer)(event.getArgs()[0])+1;
            if (_nextActionId < _config.getActionCount()) {
                _bangobj.setActionId(_nextActionId);
            }
        }
    }

    @Override // documentation inherited
    protected long getMaxScenarioTime ()
    {
        // tutorials don't normally expire after a set time, but we do end them
        // eventually if the player dallies too long
        return 30 * 60 * 1000L;
    }

    protected void processAction (int actionId)
    {
        TutorialConfig.Action action = _config.getAction(actionId);

        if (action instanceof TutorialConfig.AddUnit) {
            TutorialConfig.AddUnit aua = (TutorialConfig.AddUnit)action;
            Unit unit = Unit.getUnit(aua.type);
            unit.assignPieceId();
            unit.init();
            unit.owner = aua.owner;
            unit.position(aua.location[0], aua.location[1]);
            _bangobj.addToPieces(unit);
            _bangobj.board.updateShadow(null, unit);
        }
    }

    protected AttributeChangeListener _acl = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(BangObject.ACTION_ID)) {
                processAction(event.getIntValue());
            }
        }
    };

    protected TutorialConfig _config;
    protected BangObject _bangobj;
    protected int _nextActionId;
}
