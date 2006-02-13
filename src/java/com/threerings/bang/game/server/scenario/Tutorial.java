//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;
import java.util.HashMap;

import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.game.util.TutorialUtil;

import static com.threerings.bang.Log.log;

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
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> markers,
                                PointSet bonusSpots, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, markers, bonusSpots, purchases);

        // register to receive various tutorial specific messages
        _bangmgr.registerMessageHandler(TutorialCodes.ACTION_PROCESSED, this);

        // load up the tutorial configuraton
        BangConfig bconfig = (BangConfig)_bangmgr.getConfig();
        _config = TutorialUtil.loadTutorial(
            BangServer.rsrcmgr, bconfig.scenarios[0]);

        // set up our game object listeners; we only ever have one round in a
        // scenario, so this is OK to do in roundWillStart()
        _bangobj = bangobj;
        _bangobj.addListener(_acl);

        // start by executing the zeroth action
        _bangobj.setActionId(0);
    }

    @Override // documentation inherited
    public void tick (BangObject bangobj, short tick)
    {
        super.tick(bangobj, tick);

        // end the scenario if we've reached the last action
        if (_nextActionId >= _config.getActionCount()) {
            bangobj.setLastTick(tick);
        }
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
    protected short getBaseDuration ()
    {
        // tutorials don't normally expire after a set time, but we do end them
        // eventually if the player dallies too long
        return 400;
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

            // map the unit by id if asked to do so
            if (!StringUtil.isBlank(aua.id)) {
                _units.put(aua.id, unit);
            }

        } else if (action instanceof TutorialConfig.MoveUnit) {
            TutorialConfig.MoveUnit mua = (TutorialConfig.MoveUnit)action;
            Unit unit = _units.get(mua.id);
            Unit target = null;
            if (!StringUtil.isBlank(mua.target)) {
                target = _units.get(mua.target);
            }
            try {
                _bangmgr.moveAndShoot(
                    unit, mua.location[0], mua.location[1], target);
            } catch (InvocationException ie) {
                log.warning("Unable to execute action " + mua + ":" +
                            ie.getMessage());
            }
            
        } else if (action instanceof TutorialConfig.AddBonus) {
            TutorialConfig.AddBonus aba = (TutorialConfig.AddBonus)action;
            BonusConfig bconfig = BonusConfig.getConfig(aba.type);
            Bonus bonus = Bonus.createBonus(bconfig);
            bonus.assignPieceId();
            bonus.position(aba.location[0], aba.location[1]);
            _bangobj.addToPieces(bonus);
            _bangobj.board.updateShadow(null, bonus);
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
    protected HashMap<String,Unit> _units = new HashMap<String,Unit>();
}
