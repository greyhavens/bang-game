//
// $Id$

package com.threerings.bang.game.client;

import java.awt.event.ActionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;

import com.jme.input.KeyInput;
import com.jme.math.FastMath;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Multex;
import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;
import com.threerings.util.MessageBundle;

import com.threerings.parlor.game.client.GameController;

import com.threerings.bang.client.GlobalKeyManager;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.game.util.ScenarioUtil;

import static com.threerings.bang.Log.log;

/**
 * Handles the logic and flow of the client side of a game.
 */
public class BangController extends GameController
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
    public void mapCommand (int keyCode, final String command)
    {
        _ctx.getKeyManager().registerCommand(
            keyCode, new GlobalKeyManager.Command() {
            public void invoke (int keyCode) {
                handleAction(new ActionEvent(BangController.this, 0, command));
            }
        });
        _mapped.add(keyCode);
    }

    /**
     * Notes user interface events as they take place. This information is
     * passed along to the tutorial controller if one is active, so that it can
     * respond to such things.
     */
    public void postEvent (String event)
    {
        if (_tutcont != null) {
            try {
                _tutcont.handleEvent(event);
            } catch (Exception e) {
                log.log(Level.WARNING, "Tutorial controller choked on '" +
                        event + "'.", e);
            }
        }
    }

    /**
     * Called by the board view and other interface components when the mouse
     * is hovered over an item that has contextual help.
     */
    public void setHoveredItem (String item)
    {
        _view.help.setHelpItem(item);
    }

    @Override // documentation inherited
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        super.init(ctx, config);
        _ctx = (BangContext)ctx;
        _config = (BangConfig)config;

        // if this is a tutorial game, create our tutorial controller
        if (_config.tutorial) {
            _tutcont = new TutorialController();
            _tutcont.init(_ctx, _config);
        }

        // we start the new round after the player has dismissed the previous
        // round's stats dialogue and the game is reported as ready to go
        _selphaseMultex = new Multex(new Runnable() {
            public void run () {
                _view.setPhase(BangObject.SELECT_PHASE);
            }
        }, 2);

        // there's no stats dialogue when we first enter, so start with
        // that condition already satisfied
        _selphaseMultex.satisfied(Multex.CONDITION_TWO);

        mapCommand(KeyInput.KEY_SPACE, "StartChat");
        mapCommand(KeyInput.KEY_ESCAPE, "ShowOptions");
        mapCommand(KeyInput.KEY_TAB, "SelectNextUnit");
        mapCommand(KeyInput.KEY_C, "AdjustZoom");
        mapCommand(KeyInput.KEY_Q, "SwingCameraLeft");
        mapCommand(KeyInput.KEY_E, "SwingCameraRight");
        mapCommand(KeyInput.KEY_G, "ToggleGrid");
        mapCommand(KeyInput.KEY_H, "ToggleHelp");
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
        _bangobj = (BangObject)plobj;
        _bangobj.addListener(_ranklist);

        // let our tutorial controller know what's going on
        if (_tutcont != null) {
            _tutcont.willEnterPlace(_bangobj);
        }

        // determine our player index
        _pidx = _bangobj.getPlayerIndex(_ctx.getUserObject().getVisibleName());

        // we may be returning to an already started game
        if (_bangobj.state != BangObject.PRE_GAME) {
            stateDidChange(_bangobj.state);
        }
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        super.didLeavePlace(plobj);

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
        _view.chat.requestFocus();
    }

    /** Displays the in-game options view. */
    public void handleShowOptions (Object source)
    {
        if (_options == null) {
            _options = new InGameOptionsView(_ctx);
        }
        if (_options.isAdded()) {
            _ctx.getBangClient().clearPopup(_options, true);
        } else {
            _ctx.getBangClient().displayPopup(_options, true);
        }
    }

    /** Moves the camera to the next zoom level. */
    public void handleAdjustZoom (Object source)
    {
        ((GameInputHandler)_ctx.getInputHandler()).rollCamera();
    }

    /** Swings the camera around counter-clockwise. */
    public void handleSwingCameraLeft (Object source)
    {
        ((GameInputHandler)_ctx.getInputHandler()).swingCamera(-FastMath.PI/2);
    }

    /** Swings the camera around clockwise. */
    public void handleSwingCameraRight (Object source)
    {
        ((GameInputHandler)_ctx.getInputHandler()).swingCamera(FastMath.PI/2);
    }

    /** Toggles the grid in the game. */
    public void handleToggleGrid (Object source)
    {
        _view.view.toggleGrid(true);
    }

    /** Toggles the help display in the game. */
    public void handleToggleHelp (Object source)
    {
        _view.toggleHelpView(true);
    }

    /**
     * Instructs the controller to select the most sensible unit for this
     * player.
     */
    public void handleSelectNextUnit (Object source)
    {
        if (_bangobj == null || !_bangobj.isInPlay()) {
            return;
        }

        // determine which units are available for selection
        _selections.clear();
        Piece[] pieces = _bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            if ((pieces[ii].owner != _pidx) || !(pieces[ii] instanceof Unit) ||
                _view.view.hasQueuedMove(pieces[ii].pieceId) ||
                !_view.view.isSelectable(pieces[ii])) {
                continue;
            }
            _selections.add((Unit)pieces[ii]);
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
    public void moveAndFire (int pieceId, int tx, int ty, final int targetId)
    {
        final PointSet moves = new PointSet();
        moves.add(tx, ty);
        BangService.InvocationListener il =
            new BangService.InvocationListener() {
            public void requestFailed (String reason) {
                // TODO: play a sound or highlight the piece that failed
                // to move
                log.info("Thwarted! " + reason);
                _bangobj.board.dumpOccupiability(moves);

                // clear any pending shot indicator
                if (targetId != -1) {
                    _view.view.shotFailed(targetId);
                }
            }
        };
        log.info("Requesting move and fire [pid=" + pieceId +
                 ", to=+" + tx + "+" + ty + ", tid=" + targetId + "].");
        _bangobj.service.move(
            _ctx.getClient(), pieceId, (short)tx, (short)ty, targetId, il);

        // clear out our last selected unit as we want to start afresh
        _lastSelection = -1;

        // report to the tutorial controller
        if (targetId == -1) {
            postEvent(TutorialCodes.UNIT_MOVED);
        } else if (tx == Short.MAX_VALUE) {
            postEvent(TutorialCodes.UNIT_ATTACKED);
        } else {
            postEvent(TutorialCodes.UNIT_MOVE_ATTACKED);
        }
    }

    /** Handles a request to place a card. */
    public void placeCard (int cardId)
    {
        if (_bangobj == null || _bangobj.state != BangObject.IN_PLAY) {
            return;
        }

        Card card = (Card)_bangobj.cards.get(cardId);
        if (card == null) {
            log.warning("Requested to place non-existent card '" +
                        cardId + "'.");
        } else {
            // instruct the board view to activate placement mode
            _view.view.placeCard(card);
        }
    }

    /** Handles a request to activate a card. */
    public void activateCard (int cardId, int tx, int ty)
    {
        if (_bangobj.cards.get(cardId) == null) {
            log.warning("Requested to activate expired card " +
                        "[id=" + cardId + "].");
        } else {
            _bangobj.service.playCard(
                _ctx.getClient(), cardId, (short)tx, (short)ty);
        }
    }

    @Override // documentation inherited
    public void attributeChanged (AttributeChangedEvent event)
    {
        super.attributeChanged(event);

        // once the awards are set, we can display the end of game view
        if (event.getName().equals(BangObject.AWARDS)) {
            GameOverView gov = new GameOverView(_ctx, this, _bangobj);
            _ctx.getRootNode().addWindow(gov);
            gov.pack();
            gov.center();
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
        if (state == BangObject.SELECT_PHASE) {
            _selphaseMultex.satisfied(Multex.CONDITION_ONE);
            return true;

        } else if (state == BangObject.BUYING_PHASE) {
            _view.setPhase(state);
            return true;

        } else if (state == BangObject.POST_ROUND) {
            // let the view know that this round is over
            _view.endRound();

            // fade out the current board and prepare to fade in the next
            _view.view.doInterRoundMarqueeFade();
            return true;

        } else {
            return super.stateDidChange(state);
        }
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
        _view.endRound();
    }

    @Override // documentation inherited
    protected void gameDidEnd ()
    {
        super.gameDidEnd();

        // let interested parties know that the final round is over
        _view.endRound();
        if (_tutcont != null) {
            _tutcont.gameDidEnd();
        }
    }

    /**
     * Called by the board view after it has faded out the board at the end of
     * a round.
     */
    protected void interRoundFadeComplete ()
    {
        // potentially display the selection phase dialog for the next round
        _selphaseMultex.satisfied(Multex.CONDITION_TWO);
    }

    /**
     * Called by the board view after it has faded in the board and resolved
     * all of the unit animations to let us know that we're fully operational
     * and ready to play.
     */
    protected void readyForRound ()
    {
        // we re-use the playerReady mechanism to communicate this to the game
        // manager
        playerReady();
    }        

    /**
     * Called by the stats dialog when it has been dismissed.
     */
    protected void statsDismissed (boolean toTown)
    {
        // head back to the saloon or to town
        if (toTown || !_ctx.getLocationDirector().moveBack()) {
            _ctx.getLocationDirector().leavePlace();
            _ctx.getBangClient().showTownView();
        }
    }

    /**
     * Called whenever anything changes that might result in a change to the
     * relative ranking of the player.
     */
    protected void updateRank ()
    {
        if (_bangobj.points == null) {
            return;
        }

        // compute each player's total points including unscored points
        int[] points = (int[])_bangobj.points.clone();
        ScenarioUtil.computeUnscoredPoints(_bangobj, points);

        // determine each player's rank based on those points
        int[] spoints = (int[])points.clone();
        Arrays.sort(spoints);
        ArrayUtil.reverse(spoints);
        int rank = 0;
        for (int rr = 0; rr < spoints.length; rr++) {
            if (rr > 0 && spoints[rr] == spoints[rr-1]) {
                continue;
            }
            for (int ii = 0; ii < points.length; ii++) {
                if (points[ii] == spoints[rr]) {
                    _view.pstatus[ii].setRank(rank);
                }
            }
            rank++;
        }
    }

    /** Listens for game state changes and calls {@link #updateRank}. */
    protected class RankUpdater
        implements AttributeChangeListener, SetListener {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(BangObject.TICK)) {
                updateRank();
            }
        }
        public void entryAdded (EntryAddedEvent event) {
            if (event.getName().equals(BangObject.PIECES)) {
                updateRank();
            }
        }
        public void entryUpdated (EntryUpdatedEvent event) {
            if (event.getName().equals(BangObject.PIECES)) {
                updateRank();
            }
        }
        public void entryRemoved (EntryRemovedEvent event) {
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
    protected Multex _selphaseMultex;

    /** The units we cycle through when we press tab. */
    protected ArrayList<Unit> _selections = new ArrayList<Unit>();

    /** The unit id of the unit most recently selected via {@link
     * #handleSelectNextUnit}. */
    protected int _lastSelection = -1;

    /** Listens for game state changes that might indicate rank changes. */
    protected RankUpdater _ranklist = new RankUpdater();

    /** Displays basic in-game options. */
    protected InGameOptionsView _options;

    /** Keeps track of mapped keys. */
    protected ArrayIntSet _mapped = new ArrayIntSet();

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
}
