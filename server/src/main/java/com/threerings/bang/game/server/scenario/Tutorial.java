//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.List;

import com.google.inject.Inject;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.server.InvocationException;

import com.threerings.resource.ResourceManager;

import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;

import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.data.scenario.TutorialInfo;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.util.PieceSet;
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
    public void init (BangManager bangmgr, ScenarioInfo info)
    {
        // load up the tutorial configuraton
        BangConfig bconfig = (BangConfig)bangmgr.getConfig();
        _config = TutorialUtil.loadTutorial(_rsrcmgr, bconfig.getScenario(0));

        // make sure the player does not hose themself
        if (_config.respawn) {
            registerDelegate(new RespawnDelegate(2, false) {
                public void pieceWasKilled (
                    BangObject bangobj, Piece piece, int shooter, int sidx) {
                    if (piece.owner == 0) {
                        super.pieceWasKilled(bangobj, piece, shooter, sidx);
                    }
                }
            });
        }

        // create the various delegates we might need
        if (_config.ident.endsWith("cattle_rustling")) {
            registerDelegate(new CattleDelegate());
            registerDelegate(new CattleRustling.RustlingPostDelegate());
        } else if (_config.ident.endsWith("claim_jumping")) {
            registerDelegate(
                new NuggetDelegate(true, ClaimJumping.NUGGET_COUNT));
        } else if (_config.ident.endsWith("gold_rush")) {
            registerDelegate(new NuggetDelegate(false, 0));
        } else if (_config.ident.endsWith("land_grab")) {
            registerDelegate(new HomesteadDelegate());
        } else if (_config.ident.endsWith("totem_building")) {
            registerDelegate(new TotemBaseDelegate());
        } else if (_config.ident.endsWith("wendigo_attack")) {
            registerDelegate(_wendel = new WendigoDelegate());
        } else if (_config.ident.endsWith("forest_guardians")) {
            registerDelegate(new LoggingRobotDelegate());
            registerDelegate(_treedel = new TreeBedDelegate());
        } else if (_config.ident.endsWith("hero_building")) {
            registerDelegate(_herodel = new HeroDelegate());
        }

        // now that our delegates are registered we can call super.init
        super.init(bangmgr, info);
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
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
                StatType.TUTORIALS_COMPLETED, _config.ident);
        }

        // start by executing the zeroth action
        _bangobj.setActionId(0);
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        boolean validate = super.tick(bangobj, tick);

        // end the scenario if we've reached the last action
        if (_nextActionId >= _config.getActionCount()) {
            bangobj.setLastTick(tick);

            // also note that the player completed this tutorial
            PlayerObject user = (PlayerObject)_bangmgr.getPlayer(0);
            if (user != null) {
                user.stats.addToSetStat(
                    StatType.TUTORIALS_COMPLETED, _config.ident);
            }
        }
        return validate;
    }

    @Override // documentation inherited
    public boolean addBonus (BangObject bangobj, List<Piece> pieces)
    {
        // no automatic bonuses in tutorials
        return false;
    }

    @Override // documentation inherited
    public boolean shouldPayEarnings (PlayerObject user)
    {
        return _firstTime && user.stats.containsValue(StatType.TUTORIALS_COMPLETED, _config.ident);
    }

    /**
     * Handles giving an award to the player.
     */
    public void grantAward (PlayerObject user, Award award)
    {
        if (user == null || !shouldPayEarnings(user)) {
            return;
        }
        award.cashEarned += _config.scrip;

        if (_config.card != null) {
            CardItem card = null;
            for (Item item : user.inventory) {
                if (item instanceof CardItem && ((CardItem)item).getType().equals(_config.card)) {
                    card = (CardItem)item;
                    break;
                }
            }
            if (card == null) {
                card = new CardItem(user.playerId, _config.card);
            }

            for (int ii = 0; ii < 3; ii++) {
                card.addCard();
            }
            award.item = card;
        }
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
        // tutorials don't normally expire after a set time, but we do end them eventually if the
        // player dallies too long
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

            } else if (add.what.equals("cow")) {
                piece = new Cow();

            } else if (add.what.equals("unit")) {
                piece = Unit.getUnit(add.type);

            } else if (add.what.equals("bigshot")) {
                PlayerObject user = (PlayerObject)_bangmgr.getPlayer(0);
                if (user == null) {
                    log.warning("No player in tutorial, can't place Big Shot",
                                "game", _bangobj.which());
                    return false;
                }

                // locate a bigshot in the player's inventory
                String bstype = null;
                for (Item item : user.inventory) {
                    if (item instanceof BigShotItem) {
                        bstype = ((BigShotItem)item).getType();
                        break;
                    }
                }
                // if they have none, use a cavalry
                if (bstype == null) {
                    bstype = "frontier_town/cavalry";
                }
                piece = Unit.getUnit(bstype);

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
            if (piece instanceof Unit) {
                ((Unit)piece).originalOwner = add.owner;
            }
            piece.lastActed = Short.MIN_VALUE;

            switch (add.location.length) {
            case 1:
                Piece near = _bangobj.pieces.get(add.location[0]);
                if (near == null) {
                    log.warning("Can't add piece near non-existent piece " + add + ".");
                    return false;
                } else {
                    Point spot = _bangobj.board.getOccupiableSpot(near.x, near.y, 2, 4, null);
                    if (spot == null) {
                        log.warning("Can't find spot near piece", "piece", near, "add", add);
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
            Piece target = null;

            // see if we're finding a target by location
            if (targetId == 0 && mua.targetLoc[0] != Short.MAX_VALUE) {
                int dx = mua.targetLoc[0], dy = mua.targetLoc[1];
                for (Piece piece : _bangobj.pieces) {
                    if (piece.x == dx && piece.y == dy && piece.isTargetable()) {
                        targetId = piece.pieceId;
                        target = piece;
                    }
                }
            } else {
                target = _bangobj.pieces.get(targetId);
            }
            // if the target is not shootable, we want just to move next to it
            if (target != null && !unit.validTarget(_bangobj, target, false)) {
                targetId = -1;
                tx = target.x;
                ty = target.y;
                boolean foundMove = false;
                for (int ii = 0; ii < PieceCodes.DIRECTIONS.length; ii++) {
                    tx = target.x + PieceCodes.DX[ii];
                    ty = target.y + PieceCodes.DY[ii];
                    if (_bangobj.board.canOccupy(unit, tx, ty) && (tx != unit.x || ty != unit.y)) {
                        foundMove = true;
                        break;
                    }
                }
                if (!foundMove && !mua.noWarning) {
                    log.warning("Unable to locate spot near target", "tut", _config.ident,
                                "unit", unit, "target", target);
                }
            }

            try {
                _bangmgr.executeOrder(unit, tx, ty, targetId, true);
            } catch (InvocationException ie) {
                if (!mua.noWarning) {
                    log.warning("Unable to execute action " + mua + ":" + ie.getMessage());
                }
            }

        } else if (action instanceof TutorialConfig.ScenarioAction) {
            String type = ((TutorialConfig.ScenarioAction)action).type;
            if (type.equals("wendigo")) {
                _wendel.prepareWendigo(_bangobj, _bangobj.tick);

            } else if (type.equals("deploy_wendigo")) {
                _wendel.deployWendigo(_bangobj, _bangobj.tick);

            } else if (type.equals("reset_trees")) {
                _treedel.resetTrees(_bangobj, 0);

            } else if (type.equals("apply_levels")) {
                _herodel.applyLevels(_bangobj);
            }

        } else if (action instanceof TutorialConfig.SetCard) {
            ((TutorialInfo)_bangobj.scenario).cardType = ((TutorialConfig.SetCard)action).type;

        } else if (action instanceof TutorialConfig.WaitAction) {
            TutorialConfig.WaitAction wait = (TutorialConfig.WaitAction)action;
            if (TutorialCodes.TEXT_CLICKED.matches(wait.getEvent()) &&
                    wait.allowAttack().length == 2) {
                _bangmgr.clearOrders();
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

    // used for the Wengido Attack tutorial
    protected WendigoDelegate _wendel;

    // used for the Forest Guardians tutorial
    protected TreeBedDelegate _treedel;

    // used for the Hero Building tutorial
    protected HeroDelegate _herodel;

    // dependencies
    @Inject protected ResourceManager _rsrcmgr;
}
