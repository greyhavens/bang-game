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
import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.KeyListener;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Multex;
import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;
import com.threerings.util.MessageBundle;

import com.threerings.parlor.game.client.GameController;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.game.util.ScenarioUtil;
import com.threerings.bang.util.BangContext;

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
    public void mapCommand (int keyCode, String command)
    {
        _keycmds.put(keyCode, command);
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

        // wire up our command listener
        _view.addListener(new KeyListener() {
            public void keyPressed (KeyEvent event) {
                String cmd = (String)_keycmds.get(event.getKeyCode());
                if (cmd != null) {
                    handleAction(new ActionEvent(this, 0, cmd));
                }
            }
            public void keyReleased (KeyEvent event) {
            }
        });

        mapCommand(KeyInput.KEY_SPACE, "StartChat");
        mapCommand(KeyInput.KEY_ESCAPE, "ShowOptions");
        mapCommand(KeyInput.KEY_TAB, "SelectNextUnit");
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
        BodyObject me = (BodyObject)_ctx.getClient().getClientObject();
        _pidx = _bangobj.getPlayerIndex(me.getVisibleName());

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
    }

    /** Handles a request to leave the game. Generated by the {@link
     * #BACK_TO_LOBBY} command. */
    public void handleBackToLobby (Object source)
    {
        _ctx.getLocationDirector().moveBack();
    }

    /** Instructs the controller to activate the chat input. */
    public void handleStartChat (Object source)
    {
        _view.chat.requestFocus();
    }

    /** Instructs the controller to display the in-game options view. */
    public void handleShowOptions (Object source)
    {
        InGameOptionsView oview = new InGameOptionsView(_ctx);
        _ctx.getRootNode().addWindow(oview);
        oview.pack();
        oview.center();
    }

    /**
     * Instructs the controller to select the most sensible unit for this
     * player.
     */
    public void handleSelectNextUnit (Object source)
    {
        if (_bangobj == null || !_bangobj.inPlay()) {
            return;
        }

        // determine which units are available for selection
        _selections.clear();
        Piece[] pieces = _bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            if ((pieces[ii].owner != _pidx) ||
                !(pieces[ii] instanceof Unit) ||
                _view.view.hasQueuedMove(pieces[ii].pieceId)) {
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

        // once the awards are set, we can display the end of game stats
        if (event.getName().equals(BangObject.AWARDS)) {
            StringBuffer winners = new StringBuffer();
            for (int ii = 0; ii < _bangobj.winners.length; ii++) {
                if (_bangobj.winners[ii]) {
                    if (winners.length() > 0) {
                        winners.append(", ");
                    }
                    winners.append(_bangobj.players[ii]);
                }
            }
            String title = MessageBundle.tcompose("m.game_over_stats", winners);
            title = _ctx.xlate(GameCodes.GAME_MSGS, title);
            StatsDisplay stats =
                new StatsDisplay(_ctx, this, _bangobj, _pidx, title);
            _ctx.getRootNode().addWindow(stats);
            stats.pack();
            stats.center();
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

            // create the end of round stats display
            String title = _ctx.xlate(GameCodes.GAME_MSGS, "m.round_over_stats");
            StatsDisplay stats =
                new StatsDisplay(_ctx, this, _bangobj, _pidx, title);
            _ctx.getRootNode().addWindow(stats);
            stats.pack();
            stats.center();
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
     * Called by the stats dialog when it has been dismissed.
     */
    protected void statsDismissed ()
    {
        // if the game is over, head back to the lobby
        if (_bangobj.state == BangObject.GAME_OVER) {
            if (!_ctx.getLocationDirector().moveBack()) {
                _ctx.getLocationDirector().leavePlace();
                _ctx.getBangClient().showTownView();
            }

        } else {
            // otherwise potentially display the selection phase dialog
            // for the next round
            _selphaseMultex.satisfied(Multex.CONDITION_TWO);
        }
    }

    /**
     * Called whenever anything changes that might result in a change to the
     * relative ranking of the player.
     */
    protected void updateRank ()
    {
        if (_bangobj.funds == null) {
            return;
        }

        // compute each player's total funds including unscored funds
        int[] funds = (int[])_bangobj.funds.clone();
        ScenarioUtil.computeUnscoredFunds(_bangobj, funds);

        // determine each player's rank based on those funds
        int[] sfunds = (int[])funds.clone();
        Arrays.sort(sfunds);
        ArrayUtil.reverse(sfunds);
        int rank = 0;
        for (int rr = 0; rr < sfunds.length; rr++) {
            if (rr > 0 && sfunds[rr] == sfunds[rr-1]) {
                continue;
            }
            for (int ii = 0; ii < funds.length; ii++) {
                if (funds[ii] == sfunds[rr]) {
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

    /** Maps keys to controller commands. */
    protected HashIntMap _keycmds = new HashIntMap();

    /** The units we cycle through when we press tab. */
    protected ArrayList<Unit> _selections = new ArrayList<Unit>();

    /** The unit id of the unit most recently selected via {@link
     * #handleSelectNextUnit}. */
    protected int _lastSelection = -1;

    /** Listens for game state changes that might indicate rank changes. */
    protected RankUpdater _ranklist = new RankUpdater();

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
