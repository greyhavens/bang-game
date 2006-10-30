//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.data.Item;
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
import com.threerings.bang.game.data.piece.PieceCodes;
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
{
    /**
     * Called by the client when it has processed a particular tutorial action.
     */
    public void actionProcessed (PlayerObject caller, int actionId)
    {
        _nextActionId = actionId+1;
        if (_nextActionId < _config.getActionCount()) {
            _bangobj.setActionId(_nextActionId);
        }
    }

    @Override // documentation inherited
    public void init (BangManager bangmgr)
    {
        // load up the tutorial configuraton
        BangConfig bconfig = (BangConfig)bangmgr.getConfig();
        _config = TutorialUtil.loadTutorial(
            BangServer.rsrcmgr, bconfig.scenarios[0]);

        // create the various delegates we might need
        if (_config.ident.equals("cattle_rustling")) {
            registerDelegate(new CattleDelegate());
            registerDelegate(new CattleRustling.RustlingPostDelegate());
        } else if (_config.ident.equals("claim_jumping")) {
            registerDelegate(
                new NuggetDelegate(true, ClaimJumping.NUGGET_COUNT));
        } else if (_config.ident.equals("gold_rush")) {
            registerDelegate(new NuggetDelegate(false, 0));
        } else if (_config.ident.equals("land_grab")) {
            registerDelegate(new HomesteadDelegate());
        } else if (_config.ident.equals("totem_building")) {
            registerDelegate(new TotemBaseDelegate());
        }

        // now that our delegates are registered we can call super.init
        super.init(bangmgr);
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

        // set up our game object listeners; we only ever have one round in a
        // scenario, so this is OK to do in roundWillStart()
        _bangobj = bangobj;
        _bangobj.addListener(_acl);

        // determine whether this is the player's first time for this tutorial
        PlayerObject user = (PlayerObject)_bangmgr.getPlayer(0);
        if (user != null) {
            _firstTime = !user.stats.containsValue(
                Stat.Type.TUTORIALS_COMPLETED, _config.ident);
        }

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

    @Override // documentation inherited
    public boolean shouldPayEarnings (PlayerObject user)
    {
        return _firstTime && user.stats.containsValue(
            Stat.Type.TUTORIALS_COMPLETED, _config.ident);
    }

    @Override // documentation inherited
    public long getTickTime (BangConfig config, BangObject bangobj)
    {
        // hard code ticks at four seconds for tutorials
        return 4000L;
    }
    
    @Override // documentation inherited
    protected short getBaseDuration ()
    {
        // tutorials don't normally expire after a set time, but we do end them
        // eventually if the player dallies too long
        return 4000;
    }

    protected boolean processAction (int actionId)
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

            } else if (add.what.equals("bigshot")) {
                PlayerObject user = (PlayerObject)_bangmgr.getPlayer(0);
                if (user == null) {
                    log.warning("No player in tutorial, can't place Big Shot " +
                                "[game=" + _bangobj.which() + "].");
                    return false;
                }

                // locate a bigshot in the player's inventory
                BigShotItem bsitem = null;
                for (Item item : user.inventory) {
                    if (item instanceof BigShotItem) {
                        bsitem = (BigShotItem)item;
                        break;
                    }
                }
                if (bsitem == null) {
                    log.warning("Player has no Big Shot in tutorial " +
                                "[game=" + _bangobj.which() +
                                ", user=" + user.who() + "].");
                    return false;
                }
                piece = Unit.getUnit(bsitem.getType());

            } else {
                log.warning("Requested to add unknown piece type " + add + ".");
                return false;
            }

            // use a particular id if asked to do so
            if (add.id > 0) {
                piece.pieceId = add.id;
            } else {
                piece.assignPieceId(_bangobj);
            }
            piece.init();
            piece.setOwner(_bangobj, add.owner);
            piece.lastActed = Short.MIN_VALUE;
            switch (add.location.length) {
            case 1:
                Piece near = _bangobj.pieces.get(add.location[0]);
                if (near == null) {
                    log.warning("Can't add piece near non-existent piece " +
                                add + ".");
                    return false;
                } else {
                    Point spot = _bangobj.board.getOccupiableSpot(
                        near.x, near.y, 2);
                    if (spot == null) {
                        log.warning("Can't find spot near piece " +
                                    "[piece=" + near + ", add=" + add + "].");
                        return false;
                    } else {
                        piece.position(spot.x, spot.y);
                    }
                }
                break;

            case 2:
                piece.position(add.location[0], add.location[1]);
                break;
            }
            _bangmgr.addPiece(piece);

        } else if (action instanceof TutorialConfig.MoveUnit) {
            TutorialConfig.MoveUnit mua = (TutorialConfig.MoveUnit)action;
            Unit unit = (Unit)_bangobj.pieces.get(mua.id);
            int tx = mua.location[0], ty = mua.location[1];
            int targetId = mua.target;

            // if the target is not shootable, we want just to move next to it
            Piece target = _bangobj.pieces.get(targetId);
            if (!unit.validTarget(_bangobj, target, false)) {
                targetId = -1;
                tx = target.x;
                ty = target.y;
                boolean foundMove = false;
                for (int ii = 0; ii < PieceCodes.DIRECTIONS.length; ii++) {
                    tx = target.x + PieceCodes.DX[ii];
                    ty = target.y + PieceCodes.DY[ii];
                    if (_bangobj.board.canOccupy(unit, tx, ty) &&
                            (tx != unit.x || ty != unit.y)) {
                        foundMove = true;
                        break;
                    }
                }
                if (!foundMove) {
                    log.warning("Unable to locate spot near target " +
                        "[tut=" + _config.ident + ", unit=" + unit +
                        ", target=" + target + "].");
                }
            }

            try {
                _bangmgr.executeOrder(unit, tx, ty, targetId, true);
            } catch (InvocationException ie) {
                log.warning("Unable to execute action " + mua + ":" +
                            ie.getMessage());
            }
        }

        return (action instanceof TutorialConfig.WaitAction);
    }

    protected AttributeChangeListener _acl = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(BangObject.ACTION_ID)) {
                int actionId = event.getIntValue();
                while (!processAction(actionId)) {
                    actionId++;
                }
            }
        }
    };

    protected TutorialConfig _config;
    protected BangObject _bangobj;
    protected int _nextActionId;
    protected boolean _firstTime = false;
}
