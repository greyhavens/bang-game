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
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Cow;
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
            _bangmgr.startPhase(BangObject.PRE_TUTORIAL);
            break;

        case BangObject.PRE_TUTORIAL:
            _bangmgr.startPhase(BangObject.IN_PLAY);
            break;

        default:
            super.startNextPhase(bangobj);
            break;
        }
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> starts,
                                PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // assign claims in case this is a claim jumping tutorial
        assignClaims(bangobj, starts, ClaimJumping.NUGGET_COUNT);

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

            // also note that the player completed this tutorial
            PlayerObject user = (PlayerObject)_bangmgr.getPlayer(0);
            if (user != null) {
                user.stats.addToSetStat(
                    Stat.Type.TUTORIALS_COMPLETED, _config.ident);
            }
        }
    }

    @Override // documentation inherited
    public boolean addBonus (BangObject bangobj, Piece[] pieces)
    {
        // no automatic bonuses in tutorials
        return false;
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
        return 4000;
    }

    protected void processAction (int actionId)
    {
        TutorialConfig.Action action = _config.getAction(actionId);

        if (action instanceof TutorialConfig.AddPiece) {
            TutorialConfig.AddPiece add = (TutorialConfig.AddPiece)action;
            Piece piece;
            if (add.what.equals("bonus")) {
                BonusConfig bconfig = BonusConfig.getConfig(add.type);
                piece = Bonus.createBonus(bconfig);

            } else if (add.what.equals("unit")) {
                if (add.type.equals("cow")) {
                    piece = new Cow();
                } else {
                    piece = Unit.getUnit(add.type);
                }

            } else {
                log.warning("Requested to add unknown piece type " + add + ".");
                return;
            }

            // use a particular id if asked to do so
            if (add.id > 0) {
                piece.pieceId = add.id;
            } else {
                piece.assignPieceId(_bangobj);
            }
            piece.init();
            piece.owner = add.owner;
            piece.position(add.location[0], add.location[1]);
            _bangobj.addToPieces(piece);
            _bangobj.board.shadowPiece(piece);

        } else if (action instanceof TutorialConfig.MoveUnit) {
            TutorialConfig.MoveUnit mua = (TutorialConfig.MoveUnit)action;
            Unit unit = (Unit)_bangobj.pieces.get(mua.id);
            Unit target = (Unit)_bangobj.pieces.get(mua.target);
            try {
                _bangmgr.executeOrder(
                    unit, mua.location[0], mua.location[1], target, true);
            } catch (InvocationException ie) {
                log.warning("Unable to execute action " + mua + ":" +
                            ie.getMessage());
            }
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
