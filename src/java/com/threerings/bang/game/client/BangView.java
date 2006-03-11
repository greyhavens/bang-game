//
// $Id$

package com.threerings.bang.game.client;

import com.jme.input.KeyInput;
import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BWindow;
import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;

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

    /** Our chat display. */
    public OverlayChatView chat;

    /** Our player status views. */
    public PlayerStatusView[] pstatus;

    /** Our unit status view. */
    public UnitStatusView ustatus;

    /** A window that displays contextual help. */
    public ContextHelpView help;

    /** Creates the main panel and its sub-interfaces. */
    public BangView (BangContext ctx, BangController ctrl)
    {
        super(ctx.getStyleSheet(), new BorderLayout());

        _ctx = ctx;
        _ctrl = ctrl;

        // create our various displays
        add(view = new BangBoardView(ctx, ctrl), BorderLayout.CENTER);
        chat = new OverlayChatView(ctx);
        _timer = new RoundTimerView(ctx);
        help = new ContextHelpView(ctx);
    }

    /**
     * Configures the interface for the specified phase. If the main game
     * display has not yet been configured, this will trigger that
     * configuration as well.
     */
    public void setPhase (int phase)
    {
        // if we're not added to the UI hierarchy yet, we need to hold off a
        // moment
        if (!isAdded()) {
            _pendingPhase = phase;
            return;
        }
        _pendingPhase = -1;

        BangConfig config = (BangConfig)_ctrl.getPlaceConfig();
        BodyObject me = (BodyObject)_ctx.getClient().getClientObject();
        int pidx = _bangobj.getPlayerIndex(me.getVisibleName());

        // we call this at the beginning of each phase because the scenario
        // might decide to skip the selection or buying phase, so we need to
        // prepare prior to starting whichever phase is first
        if (!_prepared) {
            // tell the board view to start the game so that we can see the
            // board while we're buying pieces
            view.prepareForRound(_bangobj, config, pidx);

            // let the camera handler know that we're getting ready to start
            GameInputHandler gih = (GameInputHandler)_ctx.getInputHandler();
            gih.prepareForRound(this, _bangobj, pidx);

            // note that we've prepared
            _prepared = true;
        }

        switch (phase) {
        case PRE_SELECT_PHASE:
            view.doPreSelectBoardTour();
            break;

        case BangObject.SELECT_PHASE:
            setOverlay(new SelectionView(_ctx, _ctrl, config, _bangobj, pidx));
            // because we may be setting it after updating but before
            // rendering, we need make sure it's valid
            _oview.validate();
            break;

        case BangObject.BUYING_PHASE:
            ((SelectionView)_oview).setPickTeamMode(config);
            break;

        case BangObject.IN_PLAY:
            if (!config.tutorial) {
                showRoundTimer();
                showUnitStatus();
            }
            clearOverlay();
            view.startRound();
            break;
        }
    }

    /**
     * Called by the controller when a round ends.
     */
    public void endRound ()
    {
        ((GameInputHandler)_ctx.getInputHandler()).endRound(this);
        view.endRound();

        // note that we'll need to prepare for the next round
        _prepared = false;

        // remove the unit status view in between rounds
        if (ustatus.isAdded()) {
            _ctx.getRootNode().removeWindow(ustatus);
            ustatus = null;
        }
    }

    /**
     * Toggles the display of the contextual help window.
     */
    public void toggleHelpView (boolean persist)
    {
        if (help.isAdded()) {
            _ctx.getRootNode().removeWindow(help);
        } else {
            _ctx.getRootNode().addWindow(help);
            help.setHelpItem(null);
            help.pack(300, -1);
            help.setLocation(
                _ctx.getDisplay().getWidth() - help.getWidth() - 5, 75);
        }
        if (persist) {
            BangPrefs.config.setValue("context_help", help.isAdded());
        }
    }

    // documentation inherited from interface
    public void willEnterPlace (PlaceObject plobj)
    {
        _bangobj = (BangObject)plobj;

        // create our player status displays
        int pcount = _bangobj.players.length;
        _pswins = new BWindow[pcount];
        pstatus = new PlayerStatusView[pcount];
        for (int ii = 0; ii < pcount; ii++) {
            _pswins[ii] = new BWindow(
                _ctx.getStyleSheet(), GroupLayout.makeHStretch());
            _pswins[ii].setLayer(1);
            _pswins[ii].setStyleClass("player_status_win");
            _pswins[ii].add(
                pstatus[ii] = new PlayerStatusView(_ctx, _bangobj, _ctrl, ii));
        }

        // create our unit status display
        ustatus = new UnitStatusView(_ctx, _bangobj);

        // initialize the round timer
        _timer.init(_bangobj);

        chat.willEnterPlace(plobj);
    }

    // documentation inherited from interface
    public void didLeavePlace (PlaceObject plobj)
    {
        chat.didLeavePlace(plobj);

        // remove our displays
        for (int ii = 0; ii < _pswins.length; ii++) {
            if (_pswins[ii].isAdded()) {
                _ctx.getRootNode().removeWindow(_pswins[ii]);
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
        if (help.isAdded()) {
            _ctx.getRootNode().removeWindow(help);
        }

        if (_oview != null) {
            _ctx.getRootNode().removeWindow(_oview);
            _oview = null;
        }
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // go ahead and add the chat view now, other views will be added after
        // we do the board tour and unit selection
        BangConfig config = (BangConfig)_ctrl.getPlaceConfig();
        if (!config.tutorial) {
            showChat();
        }

        // finally if we were waiting to start things up, get going
        if (_pendingPhase != -1) {
            setPhase(_pendingPhase);
        }

        // start with help showing if requested and if we're not in a tutorial
        if (!config.tutorial &&
            BangPrefs.config.getValue("context_help", false)) {
            toggleHelpView(false);
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
    }

    protected void showUnitStatus ()
    {
        if (!ustatus.isAdded()) {
            _ctx.getRootNode().addWindow(ustatus);
            ustatus.reposition();
        }
    }

    protected void showPlayerStatus ()
    {
        if (_pswins[0].isAdded()) {
            return;
        }

        int width = _ctx.getDisplay().getWidth();
        int height = _ctx.getDisplay().getHeight();
        int gap = 0, wcount = _pswins.length;

        for (int ii = 0; ii < wcount; ii++) {
            BWindow pwin = _pswins[ii];
            _ctx.getRootNode().addWindow(pwin);
            pwin.pack();
            // compute the gap once we've laid out our first status window
            int wwidth = _pswins[ii].getWidth();
            if (gap == 0) {
                gap = ((width - 10) - (wcount * wwidth)) / (wcount-1);
            }
            pwin.setLocation(5 + (wwidth+gap) * ii, 5);
        }
    }

    protected void showRoundTimer ()
    {
        if (!_timer.isAdded()) {
            int height = _ctx.getDisplay().getHeight();
            _ctx.getRootNode().addWindow(_timer);
            _timer.pack();
            _timer.setLocation(0, height - _timer.getHeight());
        }
    }

    protected void showChat ()
    {
        if (!chat.isAdded()) {
            int width = _ctx.getDisplay().getWidth();
            _ctx.getRootNode().addWindow(chat);
            chat.pack();
            chat.setBounds(5, 50, width - 10, chat.getHeight());
        }
    }

    protected void setOverlay (BWindow overlay)
    {
        clearOverlay();
        _oview = overlay;
        _ctx.getRootNode().addWindow(_oview);
        _oview.pack();
        _oview.center();
    }

    protected void clearOverlay ()
    {
        if (_oview != null) {
            _ctx.getRootNode().removeWindow(_oview);
            _oview = null;
        }
    }

    /** Giver of life and context. */
    protected BangContext _ctx;

    /** Our game controller. */
    protected BangController _ctrl;

    /** The main game distributed object. */
    protected BangObject _bangobj;

    /** Displays the end of round timer. */
    protected RoundTimerView _timer;

    /** Contain per-player displays. */
    protected BWindow[] _pswins;

    /** Any window currently overlayed on the board. */
    protected BWindow _oview;

    /** Keeps track of whether we've prepared for the current round. */
    protected boolean _prepared;

    /** If we were requested to start a particular phase before we were added
     * to the interface hierarchy, that will be noted here. */
    protected int _pendingPhase = -1;
}
