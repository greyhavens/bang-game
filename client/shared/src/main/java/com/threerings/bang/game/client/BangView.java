//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.jme.scene.Controller;

import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.Histogram;
import com.samskivert.util.Interval;
import com.samskivert.util.StringUtil;

import com.threerings.jme.effect.WindowSlider;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.chat.client.OverlayChatView;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.BoardData;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.ModifiableDSet;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;

import static com.threerings.bang.Log.log;

/**
 * Manages the primary user interface during the game.
 */
public class BangView extends BWindow
    implements PlaceView
{
    /** Special pre-selection phase phase. */
    public static final int PRE_SELECT_PHASE = 0;

    /** Displays our board. */
    public BangBoardView view;

    /** The tutorial window (or null if we're not in a tutorial). */
    public BWindow tutwin;

    /** Our chat display. */
    public OverlayChatView chat;

    /** Our player status views. */
    public PlayerStatusView[] pstatus;

    /** Our unit status view. */
    public UnitStatusView ustatus;

    /** Displays bounty criteria. */
    public InGameBountyView critview;

    /** Our scenario HUD. */
    public ScenarioHUD scenHUD;

    /** Creates the main panel and its sub-interfaces. */
    public BangView (BangContext ctx, BangController ctrl)
    {
        super(ctx.getStyleSheet(), new BorderLayout());

        _ctx = ctx;
        _ctrl = ctrl;
        _perftrack = new PerformanceTracker();

        // create our various displays
        add(view = new BangBoardView(ctx, ctrl), BorderLayout.CENTER);
        chat = new OverlayChatView(ctx);
        _timer = new RoundTimerView(ctx);
    }

    /** Returns a reference to the game controller. */
    public BangController getController ()
    {
        return _ctrl;
    }

    /**
     * Configures the interface for the specified phase. If the main game display has not yet been
     * configured, this will trigger that configuration as well.
     */
    public boolean setPhase (int phase)
    {
        // if we're not added to the UI hierarchy yet, we need to hold off a moment
        if (!isAdded() || _preparing) {
            _pendingPhase = phase;
            return false;
        }
        _pendingPhase = -1;

        BangConfig config = (BangConfig)_ctrl.getPlaceConfig();
        BodyObject me = (BodyObject)_ctx.getClient().getClientObject();
        int pidx = _bangobj.getPlayerIndex(me.getVisibleName());

        if (_oview == _connecting) {
            clearOverlay();
        }

        // we call this at the beginning of each phase because the scenario might decide to skip
        // the selection phase, so we need to prepare prior to starting whichever phase is first
        if (!_prepared && !prepareForRound(config, pidx)) {
            _pendingPhase = phase;
            return false; // will be called again when preparation is finished
        }

        switch (phase) {
        case PRE_SELECT_PHASE:
            log.info("Starting Pre Select Phase");
            view.doPreSelectBoardTour();
            break;

        case BangObject.SELECT_PHASE:
            if (_bangobj.isActivePlayer(pidx)) {
                log.info("Starting Select Phase");
                SelectionView sview = new SelectionView(_ctx, this, config, _bangobj, pidx);
                if (sview.shouldAdd()) {
                    setOverlay(sview);
                    // because we may be setting it after updating but before rendering, we need
                    // make sure it's valid
                    _oview.validate();
                }
            }
            break;

        case BangObject.IN_PLAY:
            log.info("Starting In Play Phase");
            switch (config.type) {
            case PRACTICE:
                showPractice();
                if (_bangobj.isActivePlayer(pidx)) {
                    showUnitStatus();
                }
                break;
            case TUTORIAL:
                // show nothing
                break;
            default:
                showRoundTimer();
                if (_bangobj.isActivePlayer(pidx)) {
                    showUnitStatus();
                }
                break;
            }
            clearOverlay();
            view.startRound();
            _perftrack.start();
            break;

        case BangObject.POST_ROUND:
            log.info("Starting Post Round Phase");
            endRound();
            break;
        }
        return true;
    }

    /**
     * Called by the controller when a round ends.
     */
    public void endRound ()
    {
        ((GameCameraHandler)_ctx.getCameraHandler()).endRound();
        ((GameInputHandler)_ctx.getInputHandler()).endRound(this);
        view.endRound();

        // note that we'll need to prepare for the next round
        _prepared = false;

        // remove the unit status view in between rounds
        if (ustatus != null && ustatus.isAdded()) {
            _ctx.getRootNode().removeWindow(ustatus);
            ustatus = null;
        }

        // end our performance tracker and report our stats to the server
        _perftrack.end();
    }

    /**
     * Returns the number of pixels in the y direction that a window should be moved up so that it
     * appears centered above the player status views.
     */
    public int getCenterOffset ()
    {
        if (pstatus == null || pstatus[0] == null || !pstatus[0].isAdded()) {
            return 0;
        } else {
            // offset by half the height of the player status views which we'd just get from the
            // view themselves but they tend not to be laid out when we need this information
            return (5+70)/2;
        }
    }

    /**
     * Animates a card played either on a player or on the entire board.
     *
     * @param pidx the index of the card's target, or -1 for the entire board
     */
    public void showCardPlayed (Card card, int pidx)
    {
        final BWindow window = new BWindow(_ctx.getStyleSheet(), new BorderLayout());
        BLabel label = new BLabel(
            new ImageIcon(_ctx.loadImage(card.getIconPath(pidx == -1 ? "card" : "icon"))));
        if (pidx != -1) {
            label.setStyleClass(card.getStyle());
        }
        window.add(label, BorderLayout.CENTER);
        window.setLayer(2);
        _ctx.getRootNode().addWindow(window);
        window.pack();
        if (pidx == -1) {
            window.center();
        } else {
            window.setLocation(pstatus[pidx].getAbsoluteX() + 103,
                               pstatus[pidx].getAbsoluteY() + 15);
        }
        final int ox = window.getX(), oy = window.getY(), height = (pidx == -1) ? 200 : 100;
        _ctx.getRootNode().addController(new Controller() {
            public void update (float time) {
                if ((_elapsed += time) >= CARD_FALL_DURATION + CARD_LINGER_DURATION +
                    CARD_FADE_DURATION) {
                    _ctx.getRootNode().removeWindow(window);
                    _ctx.getRootNode().removeController(this);
                } else if (_elapsed >= CARD_FALL_DURATION +
                    CARD_LINGER_DURATION) {
                    window.setAlpha(1f - (_elapsed - CARD_FALL_DURATION - CARD_LINGER_DURATION) /
                                    CARD_FADE_DURATION);
                } else if (_elapsed >= CARD_FALL_DURATION) {
                    window.setAlpha(1f);
                    window.setLocation(ox, oy);
                } else {
                    float alpha = _elapsed / CARD_FALL_DURATION;
                    window.setAlpha(alpha);
                    window.setLocation(ox, oy + (int)(height * (1f - alpha)));
                }
            }
            protected float _elapsed;
        });
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _bangobj = (BangObject)plobj;

        // initialize the round timer
        _timer.init(_bangobj);

        chat.willEnterPlace(plobj);
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        chat.didLeavePlace(plobj);

        // remove our displays
        if (_pswins != null) {
            for (int ii = 0; ii < _pswins.length; ii++) {
                if (_pswins[ii].isAdded()) {
                    _ctx.getRootNode().removeWindow(_pswins[ii]);
                }
            }
        }
        if (ustatus != null && ustatus.isAdded()) {
            _ctx.getRootNode().removeWindow(ustatus);
        }
        if (chat.isAdded()) {
            _ctx.getRootNode().removeWindow(chat);
        }
        if (_timer.isAdded()) {
            _ctx.getRootNode().removeWindow(_timer);
        }

        if (_practiceView != null) {
            _ctx.getRootNode().removeWindow(_practiceView);
            _practiceView = null;
        }
        if (_oview != null) {
            _ctx.getRootNode().removeWindow(_oview);
            _oview = null;
        }
        if (critview != null) {
            _ctx.getRootNode().removeWindow(critview);
            critview = null;
        }
        if (scenHUD != null) {
            _ctx.getRootNode().removeWindow(scenHUD);
            scenHUD = null;
        }
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // go ahead and add the chat view now, other views will be added after we do the board tour
        // and unit selection
        BangConfig config = (BangConfig)_ctrl.getPlaceConfig();
        if (config.type != BangConfig.Type.TUTORIAL) {
            showChat();
        }

        // finally if we were waiting to start things up, get going
        if (_pendingPhase != -1) {
            setPhase(_pendingPhase);
        } else if (config.type == BangConfig.Type.SALOON) {
            showConnectingWindow();
        }
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();

        // make sure that we clean up per-round state
        if (_prepared) {
            endRound();
        }

        // end our performance tracker in case we haven't already
        _perftrack.end();
    }

    /**
     * Returns a reference to the round timer.
     */
    public RoundTimerView getTimer ()
    {
        return _timer;
    }

    /**
     * Called by {@link EffectHandler} to tell us when a piece was affected.
     */
    public void pieceWasAffected (Piece piece, String effect)
    {
        if (scenHUD != null) {
            scenHUD.pieceWasAffected(piece, effect);
        }
    }

    /**
     * Creates the player status views.
     */
    protected void createPlayerStatusViews ()
    {
        if (_pswins != null) {
            return;
        }

        // create our player status displays
        BangConfig config = (BangConfig)_ctrl.getPlaceConfig();
        int pcount = _bangobj.players.length;

        _pswins = new BWindow[pcount];
        pstatus = new PlayerStatusView[pcount];
        // create the windows and the player status views
        for (int ii = 0; ii < pcount; ii++) {
            _pswins[ii] = new BWindow(_ctx.getStyleSheet(), GroupLayout.makeHStretch());
            _pswins[ii].setLayer(1);
            _pswins[ii].setStyleClass("player_status_win");
            pstatus[ii] = new PlayerStatusView(_ctx, _bangobj, config, _ctrl, ii);
        }

        // then put the status views in windows, always putting ours leftmost
        int idx = 0, pidx = _bangobj.getPlayerIndex(_ctx.getUserObject().getVisibleName());
        boolean[] added = new boolean[pcount];
        if (pidx > -1) {
            _pswins[idx++].add(pstatus[pidx]);
            added[pidx] = true;

            // players on the same team next
            for (int ii = 0; ii < pcount; ii++) {
                if (!added[ii] && _bangobj.teams[ii] == _bangobj.teams[pidx]) {
                    _pswins[idx++].add(pstatus[ii]);
                    added[ii] = true;
                }
            }
        }
        // remaining players ordered by team
        for (int ii = 0; ii < pcount; ii++) {
            if (!added[ii]) {
                _pswins[idx++].add(pstatus[ii]);
                added[ii] = true;
                for (int jj = ii + 1; jj < pcount; jj++) {
                    if (!added[jj] && _bangobj.teams[ii] == _bangobj.teams[jj]) {
                        _pswins[idx++].add(pstatus[jj]);
                        added[jj] = true;
                    }
                }
            }
        }
    }

    /**
     * Displays a connecting window while we wait for all players to arrive.
     */
    protected void showConnectingWindow ()
    {
        _connecting = new BWindow(_ctx.getStyleSheet(), GroupLayout.makeHStretch());
        _connecting.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/pregame/waiting.png"))));
        setOverlay(_connecting);
    }

    /**
     * Prepares for the coming round.
     *
     * @return true if prepared, false if waiting to receive board from server
     */
    protected boolean prepareForRound (final BangConfig config, final int pidx)
    {
        createPlayerStatusViews();

        // if the board is cached, we can continue immediately; otherwise, we
        // must request the board from the server
        String boardId = StringUtil.hexlate(_bangobj.boardHash) + ":" + _bangobj.players.length;
        BoardData bdata = _ctx.getBoardCache().loadBoard(_bangobj.boardHash);
        if (bdata != null) {
            log.info("Loaded board from cache", "board", boardId);
            continuePreparingForRound(config, pidx, bdata);
            return true;
        }

        log.info("Downloading board", "board", boardId);
        _preparing = true;
        _bangobj.service.getBoard(new BangService.BoardListener() {
            public void requestProcessed (BoardData bdata) {
                // save the board for future use and continue
                _ctx.getBoardCache().saveBoard(_bangobj.boardHash, bdata);
                continuePreparingForRound(config, pidx, bdata);
                _preparing = false;
                setPhase(_pendingPhase);
            }
            public void requestFailed (String cause) {
                _ctx.getChatDirector().displayFeedback(GameCodes.GAME_MSGS, cause);
            }
        });
        return false;
    }

    /**
     * Continues preparing for the round once we've acquired the board data.
     */
    protected void continuePreparingForRound (
        BangConfig config, int pidx, BoardData bdata)
    {
        _bangobj.board = (BangBoard)bdata.board.clone();
        _bangobj.board.applyShadowPatch(_bangobj.scenario.getIdent());

        // load the static props and assign piece ids
        _bangobj.maxPieceId = 0;
        ArrayList<Prop> props = new ArrayList<Prop>();
        ArrayList<Piece> pieces = new ArrayList<Piece>();
        for (Piece piece : bdata.pieces) {
            if (piece.removeFromBoard(_bangobj)) {
                continue;
            }
            piece = (Piece)piece.clone();
            piece.assignPieceId(_bangobj);
            piece.init();
            if (piece instanceof Prop && !((Prop)piece).isInteractive()) {
                props.add((Prop)piece);
            } else if (_bangobj.state != BangObject.IN_PLAY) {
                pieces.add(piece);
            }
        }
        _bangobj.props = props.toArray(new Prop[props.size()]);

        // if we arrived in the middle of the game, the pieces will already be configured;
        // otherwise start with the ones provided by the board
        if (_bangobj.state != BangObject.IN_PLAY) {
            _bangobj.pieces = new ModifiableDSet<Piece>(pieces.iterator());
            for (Piece update : _bangobj.boardUpdates) {
                _bangobj.pieces.updateDirect(update);
            }
        }

        // tell the board view to start the game so that we can see the board during selection
        view.prepareForRound(_bangobj, config, pidx);

        // let the camera and input handlers know that we're getting ready to start
        ((GameCameraHandler)_ctx.getCameraHandler()).prepareForRound(this, _bangobj);
        ((GameInputHandler)_ctx.getInputHandler()).prepareForRound(this, _bangobj);

        // note that we've prepared
        _prepared = true;
    }

    protected void showUnitStatus ()
    {
        if (ustatus == null) {
            ustatus = new UnitStatusView(_ctx, _ctrl, view, _bangobj);
        }
        if (!ustatus.isAdded()) {
            _ctx.getRootNode().addWindow(ustatus);
            ustatus.reposition();
        }
    }

    protected void showPlayerStatus ()
    {
        if (_pswins[0].isAdded() || !isAdded()) {
            return;
        }

        int width = _ctx.getDisplay().getWidth();
        int gap = 0, wcount = _pswins.length, tgap = 2, offset = 5;
        int tcount = wcount;
        for (int ii = 0; ii < wcount; ii++) {
            for (int jj = ii + 1; jj < wcount; jj++) {
                if (_bangobj.teams[ii] == _bangobj.teams[jj]) {
                    tcount--;
                    break;
                }
            }
        }
        if (tcount == 1) {
            tcount = wcount;
        }
        int lastpidx = 0;

        for (int ii = 0; ii < wcount; ii++) {
            BWindow pwin = _pswins[ii];
            PlayerStatusView psv = (PlayerStatusView)pwin.getComponent(0);
            _ctx.getRootNode().addWindow(pwin);
            pwin.pack();
            // compute the gap once we've laid out our first status window
            int wwidth = _pswins[ii].getWidth();
            if (gap == 0) {
                if (tcount > 1) {
                    gap = ((width - 10) - (wcount * wwidth) - tgap*(wcount - tcount)) / (tcount-1);
                }
                offset = 5;
            } else if (_bangobj.teams[lastpidx] != _bangobj.teams[psv.getPidx()]) {
                offset += gap;
            } else {
                offset += tgap;
            }
            pwin.setLocation(offset, 5);
            offset += wwidth;
            lastpidx = psv.getPidx();
        }
    }

    protected void showRoundTimer ()
    {
        if (!_timer.isAdded()) {
            int height = _ctx.getDisplay().getHeight(), width = _ctx.getDisplay().getWidth();
            _ctx.getRootNode().addWindow(_timer);
            _timer.pack();
            _timer.setLocation(width - _timer.getWidth(), height - _timer.getHeight());
        }
    }

    protected void showChat ()
    {
        if (!chat.isAdded()) {
            int width = _ctx.getDisplay().getWidth();
            _ctx.getRootNode().addWindow(chat);
            chat.pack();
            chat.setBounds(5, 80, width - 10, chat.getHeight());
        }
    }

    protected void showPractice ()
    {
        if (_practiceView == null) {
            _practiceView = new PracticeView(_ctx, _bangobj);
            _ctx.getRootNode().addWindow(_practiceView);
            _practiceView.pack();
            log.info("practice view", "height", _practiceView.getHeight(),
                     "width", _practiceView.getWidth());
            _practiceView.setLocation(
                (_ctx.getDisplay().getWidth() - _practiceView.getWidth())/2, 10);
        }
    }

    protected void showBountyCriteria ()
    {
        if (critview == null) {
            critview = new InGameBountyView(_ctx, (BangConfig)_ctrl.getPlaceConfig(), _bangobj);
            _ctx.getRootNode().addWindow(critview);
            critview.pack();
            _ctx.getInterface().attachChild(
                new WindowSlider(critview, WindowSlider.FROM_TOP_STICKY, 1f, 0, 15));
        }
    }

    protected void showScenarioHUD ()
    {
        showScenarioHUD(_bangobj.scenario.getHUD(_ctx, _bangobj));
    }

    protected void showScenarioHUD (ScenarioHUD hud)
    {
        if (scenHUD != null) {
            _ctx.getRootNode().removeWindow(scenHUD);
            scenHUD = null;
        }
        scenHUD = hud;
        if (scenHUD != null) {
            _ctx.getRootNode().addWindow(scenHUD);
            scenHUD.pack();
            _ctx.getInterface().attachChild(
                    new WindowSlider(scenHUD, WindowSlider.FROM_TOP_STICKY, 1f, 0, 15));
        }
    }

    protected void setOverlay (BWindow overlay)
    {
        boolean hadFocus = chat.hasFocus();
        clearOverlay();
        _oview = overlay;
        _ctx.getRootNode().addWindow(_oview);
        _oview.pack();
        _oview.center();
        _oview.setLocation(_oview.getX(), _oview.getY() + getCenterOffset());
        if (hadFocus) {
            chat.requestFocus(); // preserve chat focus
        }
    }

    protected void clearOverlay ()
    {
        if (_oview != null) {
            _ctx.getRootNode().removeWindow(_oview);
            _oview = null;
        }
    }

    /** Used to track FPS throughout a round and report it to the server at the round's
     * completion. */
    protected class PerformanceTracker extends Interval
    {
        public PerformanceTracker () {
            super(_ctx.getApp());
        }

        public void start () {
            _boardId = StringUtil.hexlate(_bangobj.boardHash);
            schedule(1000L, true);
        }

        public void end () {
            cancel();

            // if we've already reported, then stop here
            if (_boardId == null) {
                return;
            }

            // make sure we're not too late to the party
            if (_bangobj != null) {
                int[] histo = _perfhisto.getBuckets().clone();
                String driver = GL11.glGetString(GL11.GL_VENDOR) + ", " +
                    GL11.glGetString(GL11.GL_RENDERER) + ", " +
                    GL11.glGetString(GL11.GL_VERSION);
                _bangobj.service.reportPerformance(_boardId, driver, histo);
            }

            // if >half of the samples are below 20 fps, recommend lowering the detail level
            int[] buckets = _perfhisto.getBuckets();
            int below = buckets[0] + buckets[1];
            if (below > _perfhisto.size()/2) {
                _ctx.getBangClient().queueSuggestLowerDetail();
            }

            _perfhisto.clear();
            _boardId = null;
        }

        public void expired () {
            _perfhisto.addValue((int)_ctx.getApp().getRecentFrameRate());
        }

        protected String _boardId;
        protected Histogram _perfhisto = new Histogram(0, 10, 7);
    }

    /** Giver of life and context. */
    protected BangContext _ctx;

    /** Our game controller. */
    protected BangController _ctrl;

    /** The main game distributed object. */
    protected BangObject _bangobj;

    /** Displays the end of round timer. */
    protected RoundTimerView _timer;

    /** Displays a giant end practice button. */
    protected PracticeView _practiceView;

    /** Contain per-player displays. */
    protected BWindow[] _pswins;

    /** Any window currently overlayed on the board. */
    protected BWindow _oview;

    /** The Connecting window. */
    protected BWindow _connecting;

    /** Keeps track of whether we've prepared or are preparing for the current round. */
    protected boolean _prepared, _preparing;

    /** If we were requested to start a particular phase before we were added to the interface
     * hierarchy, that will be noted here. */
    protected int _pendingPhase = -1;

    /** Takes periodic samples of our frames per second and reports them to the server at the end
     * of the round. */
    protected PerformanceTracker _perftrack;

    /** The time it takes for a played card to fall into position. */
    protected static final float CARD_FALL_DURATION = 0.5f;

    /** The time a played card lingers in view. */
    protected static final float CARD_LINGER_DURATION = 1.25f;

    /** The time it takes for a played card to fade out. */
    protected static final float CARD_FADE_DURATION = 0.25f;
}
