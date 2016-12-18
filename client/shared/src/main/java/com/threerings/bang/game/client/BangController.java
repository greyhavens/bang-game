//
// $Id$

package com.threerings.bang.game.client;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import com.badlogic.gdx.Input.Keys;

import com.jme.math.FastMath;
import com.jmex.bui.BWindow;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Multex;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.ResultListener;
import com.samskivert.swing.event.CommandEvent;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.parlor.game.client.GameController;
import com.threerings.stats.data.StatSet;

import com.threerings.bang.client.BangMetrics;
import com.threerings.bang.client.GlobalKeyManager;
import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.client.AvatarView;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Handles the logic and flow of the client side of a game.
 */
public class BangController extends GameController
    implements BangReceiver
{
    /** The name of the command posted by the "Back to lobby" button in
     * the side bar. */
    public static final String BACK_TO_LOBBY = "BackToLobby";

    /** A command that requests to place a card. */
    public static final String PLACE_CARD = "PlaceCard";

    /**
     * Configures a controller command that will be fired when the specified
     * key is pressed (assuming no key-listening component has focus like the
     * chat box).
     */
    public void mapCommand (int keyCode, String command)
    {
        mapCommand(keyCode, new ActionEvent(BangController.this, 0, command));
    }

    /**
     * Configures a controller command that will be fired when the specified
     * key is pressed (assuming no key-listening component has focus like the
     * chat box).
     */
    public void mapCommand (int keyCode, String command, Object argument)
    {
        mapCommand(keyCode, new CommandEvent(
                    BangController.this, command, argument));
    }

    /**
     * Configures a controller command that will be fired when the specified
     * key is pressed (assuming no key-listening component has focus like the
     * chat box).
     */
    public void mapCommand (int keyCode, final ActionEvent event)
    {
        _ctx.getKeyManager().registerCommand(
            keyCode, new GlobalKeyManager.Command() {
            public void invoke (int keyCode, int modifiers) {
                handleAction(event);
            }
        });
        _mapped.add(keyCode);
    }

    /**
     * Notes user interface events as they take place. This information is
     * passed along to the tutorial controller if one is active, so that it can
     * respond to such things.
     */
    public void postEvent (String event, int id)
    {
        if (_tutcont != null) {
            try {
                _tutcont.handleEvent(event, id);
            } catch (Exception e) {
                log.warning("Tutorial controller choked on '" + event + "'.", e);
            }
        }
    }

    // documentation inherited from interface BangReceiver
    public void orderInvalidated (int unitId, String reason)
    {
        orderInvalidated(unitId, -1, reason);
    }

    @Override // documentation inherited
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        super.init(ctx, config);
        _ctx = (BangContext)ctx;
        _config = (BangConfig)config;

        log.info("Entered game " + config + ".");

        // if this is a tutorial game, create our tutorial controller
        if (_config.type == BangConfig.Type.TUTORIAL) {
            _tutcont = new TutorialController();
            _tutcont.init(_ctx, _config, _view);
        }

        // we start the new round after the player has dismissed the previous round's stats
        // dialogue and the game is reported as ready to go
        _startRoundMultex = new Multex(new Runnable() {
            public void run () {
                // we could be gone by this point
                if (_bangobj != null) {
                    roundDidStart();
                }
            }
        }, 2);

        // there's no stats dialogue when we first enter, so start with that
        // condition already satisfied
        _startRoundMultex.satisfied(Multex.CONDITION_TWO);

        // we'll use this one at the end of the game
        _postRoundMultex = new Multex(new Runnable() {
            public void run () {
                // we may be in auto-looping mode
                if (Boolean.parseBoolean(System.getProperty("loopplay"))) {
                    _ctx.getBangClient().startTestGame(false);
                    return;
                }
                // we could be gone by this point
                if (_bangobj != null) {
                    displayStatsView();
                }
            }
        }, 3);

        mapCommand(Keys.SPACE, "StartChat");
        mapCommand(Keys.ENTER, "StartChat");
        mapCommand(Keys.ESCAPE, "ShowOptions");
        mapCommand(Keys.F1, "ShowOptions");
        mapCommand(Keys.TAB, "SelectNextUnit");
        mapCommand(Keys.C, "AdjustZoom");
        mapCommand(Keys.Q, "SwingCameraLeft");
        mapCommand(Keys.E, "SwingCameraRight");
        //mapCommand(Keys.G, "ToggleGrid");
        mapCommand(Keys.NUM_1, PLACE_CARD, new Integer(0));
        mapCommand(Keys.NUM_2, PLACE_CARD, new Integer(1));
        mapCommand(Keys.NUM_3, PLACE_CARD, new Integer(2));
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        _bangobj = (BangObject)plobj;

        // generate our color lookup
        BangMetrics.generateColorLookup(_bangobj.teams);

        super.willEnterPlace(plobj);
        _bangobj.addListener(_ranklist);

        // register to receive messages from the server
        _ctx.getClient().getInvocationDirector().registerReceiver(
            new BangDecoder(this));

        // let our tutorial controller know what's going on
        if (_tutcont != null) {
            _tutcont.willEnterPlace(_bangobj);
        }

        // determine our player index
        _pidx = _bangobj.getPlayerIndex(_ctx.getUserObject().getVisibleName());

        // we may be returning to an already started game
        if (_bangobj.state != BangObject.PRE_GAME) {
            stateDidChange(_bangobj.state);
            if (!_musicStarted) {
                // start up the music for this scenario
                startScenarioMusic(3f);
            }
        }

        // if we're just observing an auto-play game, let the manager know we're ready
        if (_config.allPlayersAIs()) {
            _ctx.getClient().getRunQueue().postRunnable(new Runnable() {
                public void run () {
                    // finally let the game manager know that we're ready to roll
                    playerReady();
                }
            });
        }
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        super.didLeavePlace(plobj);

        // clear out our receiver registration
        _ctx.getClient().getInvocationDirector().unregisterReceiver(
            BangDecoder.RECEIVER_CODE);

        if (_tutcont != null) {
            _tutcont.didLeavePlace(_bangobj);
        }

        if (_bangobj != null) {
            _bangobj.removeListener(_ranklist);
            _bangobj = null;
        }

        // clear our key mappings
        for (int ii = 0; ii < _mapped.size(); ii++) {
            _ctx.getKeyManager().clearCommand(_mapped.get(ii));
        }
    }

    /** Handles a request to leave the game. Generated by the {@link
     * #BACK_TO_LOBBY} command. */
    public void handleBackToLobby (Object source)
    {
        _ctx.getLocationDirector().moveBack();
    }

    /** Activates the chat input. */
    public void handleStartChat (Object source)
    {
        if (_view.chat.isAdded()) { // chat is not available in tutorials
            _view.chat.requestFocus();
        }
    }

    /** Displays the in-game options view. */
    public void handleShowOptions (Object source)
    {
        if (_bangobj == null) {
            return;
        }
        if (_options == null) {
            _options = new InGameOptionsView(_ctx, _bangobj, _config);
        }
        if (_options.isAdded()) {
            _options.clearPopup();
        } else {
            _ctx.getBangClient().displayPopup(_options, true);
        }
    }

    /** Moves the camera to the next zoom level. */
    public void handleAdjustZoom (Object source)
    {
        if (_bangobj.tick >= 0) {
            ((GameInputHandler)_ctx.getInputHandler()).rollCamera();
        }
    }

    /** Swings the camera around counter-clockwise. */
    public void handleSwingCameraLeft (Object source)
    {
        if (_bangobj.tick >= 0) {
            ((GameInputHandler)_ctx.getInputHandler()).swingCamera(
                -FastMath.PI/2);
        }
    }

    /** Swings the camera around clockwise. */
    public void handleSwingCameraRight (Object source)
    {
        if (_bangobj.tick >= 0) {
            ((GameInputHandler)_ctx.getInputHandler()).swingCamera(
                FastMath.PI/2);
        }
    }

    /** Toggles the grid in the game. */
    public void handleToggleGrid (Object source)
    {
        _view.view.toggleGrid(true);
    }

    /** Places the first card. */
    public void handlePlaceCard (Object source, Object argument)
    {
        int value = ((Integer)argument).intValue();
        if (_view.pstatus != null && _pidx < _view.pstatus.length && _pidx > -1) {
            _view.pstatus[_pidx].playCardAtIndex(value);
        }
    }

    /**
     * Instructs the controller to select the most sensible unit for this
     * player.
     */
    public void handleSelectNextUnit (Object source)
    {
        if (_bangobj == null || !_bangobj.isActivePlayer(_pidx) ||
                !_bangobj.isInteractivePlay()) {
            return;
        }

        // determine which units are available for selection
        _selections.clear();
        for (Piece piece : _bangobj.getPieceArray()) {
            if ((piece.owner != _pidx) || !(piece instanceof Unit) ||
                _view.view.hasAdvanceOrder(piece.pieceId) ||
                !_view.view.isSelectable(piece)) {
                continue;
            }
            _selections.add((Unit)piece);
        }
        Collections.sort(_selections, UNIT_COMPARATOR);

        // nothing doing if we have no available selections
        if (_selections.size() == 0) {
            return;
        }

        // locate the index of the most recently selected unit
        int selidx = -1;
        for (int ii = 0, ll = _selections.size(); ii < ll; ii++) {
            if (_selections.get(ii).pieceId == _lastSelection) {
                selidx = ii;
                break;
            }
        }

        // select that unit and note the new selection
        Unit unit = _selections.get((selidx+1)%_selections.size());
        _lastSelection = unit.pieceId;
        _view.view.selectUnit(unit, true);
    }

    /** Handles a request to move a piece. */
    public void moveAndFire (
        final int pieceId, final int tx, final int ty, final int targetId)
    {
        final Unit unit = (Unit)_bangobj.pieces.get(pieceId);
        BangService.ResultListener rl = new BangService.ResultListener() {
            public void requestProcessed (Object result) {
                int code = (Integer)result;
                if (code == GameCodes.EXECUTED_ORDER) {
                    // report to the tutorial controller
                    if (targetId == -1) {
                        postEvent(TutorialCodes.UNIT_MOVED, pieceId);
                    } else if (tx == Short.MAX_VALUE) {
                        postEvent(TutorialCodes.UNIT_ATTACKED, pieceId);
                    } else {
                        postEvent(TutorialCodes.UNIT_MOVE_ATTACKED, pieceId);
                    }

                } else if (code == GameCodes.QUEUED_ORDER) {
                    // tell the view to display a queued move for this piece
                    _view.view.addAdvanceOrder(unit.pieceId, tx, ty, targetId);

                    // report to the tutorial controller
                    if (targetId == -1) {
                        postEvent(TutorialCodes.UNIT_ORDERED_MOVE, pieceId);
                    } else if (tx == Short.MAX_VALUE) {
                        postEvent(TutorialCodes.UNIT_ORDERED_ATTACK, pieceId);
                    } else {
                        postEvent(TutorialCodes.UNIT_ORDERED_MOVE_ATTACK, pieceId);
                    }

                } else {
                    log.warning("Got unknown response to move", "unit", unit, "tx", tx, "ty", ty,
                                "tid", targetId, "result", result);
                }

                // clear any pending shot indicator
                if (targetId != -1) {
                    _view.view.clearPendingShot(targetId);
                }
            }

            public void requestFailed (String reason) {
                // report a failed order like an invalidated advance order
                orderInvalidated(unit.pieceId, targetId, reason);
            }
        };

        log.info("Requesting move and fire", "[unit", unit, "to", "+"+tx+"+"+ty, "tid", targetId);
        _bangobj.service.order(pieceId, (short)tx, (short)ty, targetId, rl);

        // clear out our last selected unit as we want to start afresh
        _lastSelection = -1;
    }

    /** Handles a request to cancel a unit's advance order. */
    public void cancelOrder (int pieceId)
    {
        log.info("Requesting order cancellation", "pid", pieceId);
        // if the order is canceled, we'll hear about it via orderInvalidated
        _bangobj.service.cancelOrder(pieceId);
    }

    /** Handles a request to place a card. */
    public void placeCard (int cardId)
    {
        if (_bangobj == null || !_bangobj.isInteractivePlay()) {
            return;
        }

        Card card = _bangobj.cards.get(cardId);
        Card activeCard = getPlacingCard();
        if (card == null) {
            log.warning("Requested to place non-existent card '" + cardId + "'.");

        } else if (activeCard != null && activeCard.getPlacementMode() ==
                Card.PlacementMode.VS_CARD && card.owner != _pidx) {
            activateCard(activeCard.cardId, new Integer(card.cardId));

        } else if (card.getPlacementMode() == Card.PlacementMode.VS_BOARD) {
            activateCard(card.cardId, null);

        } else if (card.owner == _pidx) {
            // instruct the board view to activate placement mode
            _view.view.placeCard(card);
            postEvent(TutorialCodes.CARD_SELECTED, cardId);
        }
    }

    /** Returns the card being placed, if any. */
    public Card getPlacingCard ()
    {
        return _view.view.getCard();
    }

    /** Cancels the card placement operation. */
    public void cancelCardPlacement ()
    {
        _view.view.clearPlacingCard();
    }

    /** Handles a request to activate a card. */
    public void activateCard (final int cardId, Object target)
    {
        _view.view.clearPlacingCard();
        if (_bangobj.cards.get(cardId) == null) {
            log.warning("Requested to activate expired card", "id", cardId);
        } else {
            BangService.ConfirmListener cl = new BangService.ConfirmListener() {
                public void requestProcessed () {
                    postEvent(TutorialCodes.CARD_PLAYED, cardId);
                }
                public void requestFailed (String reason) {
                    _ctx.getChatDirector().displayFeedback(GameCodes.GAME_MSGS, reason);
                }
            };
            _bangobj.service.playCard(cardId, target, cl);
        }
    }

    /**
     * Retrieve the StatSet for the specified round.
     */
    public StatSet[] getStatSetArray (int roundId)
    {
        // the stats for the current round are in the game object, the rest are cached
        return (roundId == _bangobj.roundId) ? _bangobj.stats : _statMap.get(roundId);
    }

    /**
     * Retrieve the StatsView.
     */
    public StatsView getStatsView ()
    {
        return _statsView;
    }

    public void startScenarioMusic (float duration)
    {
        _musicStarted = true;
        _ctx.getBangClient().queueMusic(
            _bangobj.scenario.getMusic(), true, duration);
    }

    @Override // documentation inherited
    public void attributeChanged (AttributeChangedEvent event)
    {
        super.attributeChanged(event);

        // once the awards are set, we can display the end of game view
        if (event.getName().equals(BangObject.STATS) &&
            // we handle things specially in the tutorial and practice
            _config.type != BangConfig.Type.TUTORIAL && _config.type != BangConfig.Type.PRACTICE) {
            storeStats();

        } else if (event.getName().equals(BangObject.AWARDS)) {
            // generate the winner's victory pose so it will be cached when we get around to showing
            // the game over window
            if (_config.type == BangConfig.Type.SALOON) {
                int pidx = _bangobj.awards[0].pidx;
                if (_bangobj.playerInfo[pidx].victory != null) {
                    AvatarView.getImage(_ctx, _bangobj.playerInfo[pidx].victory,
                            new ResultListener.NOOP<BufferedImage>());
                }

            // for bounties, we need to wait until we get the awards before advancing our post
            // game multex
            } else {
                _postRoundMultex.satisfied(Multex.CONDITION_TWO);
            }
            _postRoundMultex.satisfied(Multex.CONDITION_THREE);

        // regenerate our color lookup when the teams change
        } else if (event.getName().equals(BangObject.TEAMS)) {
            BangMetrics.generateColorLookup(_bangobj.teams);
        }
    }

    @Override // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        _view = new BangView((BangContext)ctx, this);
        return _view;
    }

    @Override // documentation inherited
    protected boolean stateDidChange (int state)
    {
        if (state == BangObject.SELECT_PHASE ||
            state == BangObject.SKIP_SELECT_PHASE) {
            // when our inter-round fade is complete we'll start the next round
            _startRoundMultex.satisfied(Multex.CONDITION_ONE);

            // if we're a watcher and the stats view is showing, we have to start the inter round
            // board fade immediately because the game will not wait for the watcher before
            // starting the next round
            if ((_bangobj == null || !_bangobj.isActivePlayer(_pidx)) && _statsView != null) {
                _view.view.doInterRoundBoardFade();
            }
            return true;

        } else if (state == BangObject.IN_PLAY) {
            // if we're a watcher, we may not yet have dismissed our stats view, but since things
            // are starting we need to forcibly do that for them to ensure that it's gone before
            // the end of this round
            if (_statsView != null) {
                _ctx.getBangClient().clearPopup(_statsView, true);
                statsDismissed();
            }
            // we need to let the standard in-play processing happen as well
            return super.stateDidChange(state);

        } else if (state == BangObject.POST_ROUND) {
            // let the view know that this round is over
            _view.setPhase(state);
            _startRoundMultex.reset();

            // fade out the current board and prepare to fade in the next
            _view.view.doInterRoundMarqueeFade();
            return true;

        } else {
            return super.stateDidChange(state);
        }
    }

    /**
     * Called a the beginning of every round.
     */
    protected void roundDidStart ()
    {
        _bangobj.boardEffect = null;
        _bangobj.globalHindrance = null;

        // TODO: if we're in a bounty game, sneak in a custom marquee

        // display the unit selection if appropriate
        _view.setPhase(BangView.PRE_SELECT_PHASE);

        // start up the music for this scenario
        startScenarioMusic(3f);
    }

    @Override // documentation inherited
    protected void gameDidStart ()
    {
        super.gameDidStart();

        // when the game "starts", the round is ready to be played, but the
        // game only "ends" once, at the actual end of the game
        _view.setPhase(BangObject.IN_PLAY);
    }

    @Override // documentation inherited
    protected void gameWillReset ()
    {
        super.gameWillReset();

        // let the view know that the final round is over
        _view.setPhase(BangObject.POST_ROUND);
    }

    @Override // documentation inherited
    protected void gameDidEnd ()
    {
        super.gameDidEnd();

        // Let interested parties know that the final round is over.
        // If setPhase returns false it means we've just started watching,
        // so we'll jump straight to the end game stats view
        if (!_view.setPhase(BangObject.POST_ROUND)) {
            storeStats();
            _postRoundMultex.satisfied(Multex.CONDITION_ONE);
            _postRoundMultex.satisfied(Multex.CONDITION_THREE);
            return;
        }

        if (_tutcont != null) {
            _tutcont.gameDidEnd();
            // no "game over" marquee for tutorials
            postGameFadeComplete();

        } else {
            // fade in a game over marquee
            _view.view.doPostGameMarqueeFade();
        }
    }

    @Override // documentation inherited
    protected void gameWasCancelled ()
    {
        super.gameWasCancelled();

        // if the game was cancelled there were errors on the server and we are probably in a weird
        // state, so just go back to town
        if (_ctx.getBangClient().showTownView()) {
            _ctx.getChatDirector().displayFeedback(GameCodes.GAME_MSGS, "m.game_cancelled");
        }
    }

    /**
     * Called by the board view after it has swung around the board before the
     * selection phase.
     */
    protected void preSelectBoardTourComplete ()
    {
        if (_bangobj == null) {
            return;
        }
        if (_config.type != BangConfig.Type.TUTORIAL) {
            // display the player status displays
            _view.showPlayerStatus();
        }

        if (_config.type == BangConfig.Type.BOUNTY) {
            // display our bounty criterion
            if (_bangobj.isActivePlayer(_pidx)) {
                _ctx.getBangClient().displayPopup(new PreGameBountyView(
                            _ctx, this, _bangobj.bounty, _bangobj.bountyGameId, _config), true);
            }

        } else if (_config.type != BangConfig.Type.SALOON || _config.allPlayersAIs()) {
            // since we're not selecting anything, let the server know that we're ready
            playerReadyFor(BangObject.SKIP_SELECT_PHASE);

        } else if (_bangobj.state == BangObject.SELECT_PHASE) {
            // display the selection dialog
            _view.setPhase(BangObject.SELECT_PHASE);
        }
    }

    /**
     * Called by the board view after it has faded in the inter round marquee.
     */
    protected void interRoundMarqueeFadeComplete ()
    {
        _postRoundMultex.satisfied(Multex.CONDITION_ONE);
        _postRoundMultex.satisfied(Multex.CONDITION_THREE);
    }

    /**
     * Called by the board view after it has faded out the board at the end of a round.
     */
    protected void interRoundFadeComplete ()
    {
        // potentially display the selection phase dialog for the next round
        _startRoundMultex.satisfied(Multex.CONDITION_TWO);
    }

    /**
     * Called by the board view after it has faded in the game over marquee.
     */
    protected void postGameFadeComplete ()
    {
        // potentially display the post-game stats
        _postRoundMultex.satisfied(Multex.CONDITION_ONE);

        if (_config.type == BangConfig.Type.PRACTICE) {
            BangBootstrapData bbd = (BangBootstrapData)_ctx.getClient().getBootstrapData();
            _ctx.getLocationDirector().moveTo(bbd.ranchOid);
        }
    }

    /**
     * Called by the board view after it has faded in the board and resolved all of the unit
     * animations to let us know that we're fully operational and ready to play.
     */
    protected void readyForRound ()
    {
        // We could have left the game before this being called
        if (_bangobj == null) {
            return;
        }

        // reenable the camera controls now that we're fully operational
        _ctx.getInputHandler().setEnabled(true);

        // do a full GC before we sweep the camera up to tidy up after all the unit model loading
        System.gc();

        // zoom the camera to the center level
        ((GameInputHandler)_ctx.getInputHandler()).rollCamera(FastMath.PI);

        // if we're in a bounty game, slide the criteria status view down from the top
        if (_config.type == BangConfig.Type.BOUNTY) {
            _view.showBountyCriteria();
        } else if (_bangobj.isActivePlayer(_pidx)) {
            _view.showScenarioHUD();
        }

        // if we're one of the players
        if (_bangobj.isActivePlayer(_pidx) || _config.allPlayersAIs()) {
            // let the game manager know that our units are in place and we're fully ready to go
            playerReadyFor(BangObject.IN_PLAY);
        }
    }

    /**
     * Reports an invalidated order via the appropriate interfaces.
     */
    protected void orderInvalidated (int unitId, int targetId, String reason)
    {
        boolean alert = !reason.equals(GameCodes.ORDER_CLEARED) &&
            !reason.equals(GameCodes.MOVER_NO_LONGER_VALID);
        _view.view.orderInvalidated(unitId, targetId, alert);
        if (alert && _view.ustatus != null) {
            _view.ustatus.orderInvalidated(unitId, targetId);
        }
    }

    /**
     * Called when our end of round stats are received on the game object.
     */
    protected void storeStats ()
    {
        // keep the stats for this round around for later
        _statMap.put(_bangobj.roundId, _bangobj.stats);
        // create our stats view now that we have our stats; we'll hold off on showing it until
        // both post-round conditions have been met
        _statsView = _bangobj.scenario.getStatsView(_ctx);
        _statsView.init(BangController.this, _bangobj, true);
        if (_config.type == BangConfig.Type.SALOON) {
            _postRoundMultex.satisfied(Multex.CONDITION_TWO);
        }
    }

    /**
     * Called to display the appropriate end of round (or game) display.
     */
    protected void displayStatsView ()
    {
        BWindow view;
        if (_config.type == BangConfig.Type.BOUNTY) {
            view = new BountyGameOverView(_ctx, _bangobj.bounty, _bangobj.bountyGameId,
                                          _config, _bangobj, _ctx.getUserObject());
        } else if (_config.duration == BangConfig.Duration.PRACTICE) {
            view = new TutorialGameOverView(
                    _ctx, TutorialCodes.PRACTICE_PREFIX + _config.rounds.get(0).scenario,
                    _config, _bangobj, _ctx.getUserObject());

        } else {
            view = _statsView;
        }
        if (view != null) {
            _ctx.getBangClient().displayPopup(view, true);
        }
    }

    /**
     * Called by the stats dialog when it has been dismissed.
     */
    protected void statsDismissed ()
    {
        _statsView = null;

        // for players, wait for the stats to be dismissed before fading the current board out and
        // switching to the new board; for watchers, do so immediately at the end of the round
        if (_bangobj != null && _bangobj.isActivePlayer(_pidx)) {
            _view.view.doInterRoundBoardFade();
        }
    }

    /**
     * Called whenever anything changes that might result in a change to the relative ranking of
     * the player.
     */
    protected void updateRank ()
    {
        // don't update our rank until after the first tick, by which time everyone will have
        // loaded their units and we might have some points
        if (_bangobj.points == null || _bangobj.tick < 1 || _view.pstatus == null) {
            return;
        }

        // for team games, futz around with the ranks
        boolean team = _bangobj.isTeamGame();
        int[] points = team ? _bangobj.getTeamPoints(_bangobj.points) : _bangobj.points;

        // determine each player's rank based on those points
        int[] spoints = points.clone();
        Arrays.sort(spoints);
        ArrayUtil.reverse(spoints);
        int rank = -1;
        for (int rr = 0; rr < spoints.length; rr++) {
                rank++;
            if (rr > 0 && spoints[rr] == spoints[rr-1]) {
                if (team) {
                    rank--;
                }
                continue;
            }
            for (int ii = 0; ii < points.length; ii++) {
                if (points[ii] == spoints[rr]) {
                    _view.pstatus[ii].setRank(rank);
                    // update our criteria view if one is showing
                    if (ii == 0 && _view.critview != null) {
                        _view.critview.setRank(rank);
                    }
                }
            }
        }
    }

    /**
     * Reports to the server that we're ready for the specified round phase.
     */
    protected void playerReadyFor (int phase)
    {
        // if a player bails out during the pre-game we may end up getting called after we're all
        // shutdown, so just NOOP, don't NPE
        if (_bangobj != null) {
            _bangobj.manager.invoke("playerReadyFor", phase);
        }
    }

    /** Listens for game state changes and calls {@link #updateRank}. */
    protected class RankUpdater
        implements AttributeChangeListener, SetListener<DSet.Entry> {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(BangObject.TICK)) {
                updateRank();

                // display a marquee when the round is 12 ticks from ending (but not for
                // objectiveless scenarios like the tutorial)
                if (_bangobj.scenario != null && _bangobj.scenario.getObjectives() != null &&
                    _bangobj.lastTick - _bangobj.tick == ALMOST_OVER_TICKS) {
                    _view.view.fadeMarqueeInOut("m.round_will_end", 1f);
                    _view.getTimer().setEndState(true);
                }
            }
        }

        public void entryAdded (EntryAddedEvent<DSet.Entry> event) {
            if (event.getName().equals(BangObject.PIECES)) {
                updateRank();
            }
        }
        public void entryUpdated (EntryUpdatedEvent<DSet.Entry> event) {
            if (event.getName().equals(BangObject.PIECES)) {
                updateRank();
            }
        }
        public void entryRemoved (EntryRemovedEvent<DSet.Entry> event) {
            if (event.getName().equals(BangObject.PIECES)) {
                updateRank();
            }
        }
    }

    /** A casted reference to our context. */
    protected BangContext _ctx;

    /** The configuration of this game. */
    protected BangConfig _config;

    /** Contains our main user interface. */
    protected BangView _view;

    /** A casted reference to our game object. */
    protected BangObject _bangobj;

    /** A helper that exists if we're in tutorial mode. */
    protected TutorialController _tutcont;

    /** Our player index or -1 if we're not a player. */
    protected int _pidx;

    /** Used to start the new round after two conditions have been met. */
    protected Multex _startRoundMultex;

    /** Used to show the stats once we've faded in our marquee and the stats have arrived. */
    protected Multex _postRoundMultex;

    /** The last scenario played. */
    protected StatsView _statsView;

    /** The units we cycle through when we press tab. */
    protected ArrayList<Unit> _selections = new ArrayList<Unit>();

    /** The unit id of the unit most recently selected via {@link #handleSelectNextUnit}. */
    protected int _lastSelection = -1;

    /** Listens for game state changes that might indicate rank changes. */
    protected RankUpdater _ranklist = new RankUpdater();

    /** Displays basic in-game options. */
    protected InGameOptionsView _options;

    /** Keeps track of mapped keys. */
    protected ArrayIntSet _mapped = new ArrayIntSet();

    /** Stores previous round stats data. */
    protected HashIntMap<StatSet[]> _statMap = new HashIntMap<StatSet[]>();

    /** Keeps track of if we've started the music. */
    protected boolean _musicStarted;

    /** Used to by {@link #handleSelectNextUnit}. */
    protected static Comparator<Unit> UNIT_COMPARATOR = new Comparator<Unit>() {
        public int compare (Unit u1, Unit u2) {
            if (u1.lastActed != u2.lastActed) {
                return u1.lastActed - u2.lastActed;
            }
            String t1 = u1.getType(), t2 = u2.getType();
            int cv = t1.compareTo(t2);
            if (cv != 0) {
                return cv;
            }
            return u1.pieceId - u2.pieceId;
        }
    };

    /** The number of ticks before the end of the game on which we show a marquee warning the
     * player that the round is almost over. */
    protected static final int ALMOST_OVER_TICKS = 12;
}
