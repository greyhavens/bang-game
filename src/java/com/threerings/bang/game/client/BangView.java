//
// $Id$

package com.threerings.bang.game.client;

import com.jme.renderer.ColorRGBA;

import com.jme.bui.BComponent;
import com.jme.bui.BDecoratedWindow;
import com.jme.bui.BWindow;
import com.jme.bui.background.TintedBackground;
import com.jme.bui.layout.BorderLayout;
import com.jme.bui.layout.GroupLayout;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.jme.chat.ChatView;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.util.BangContext;

/**
 * Manages the primary user interface during the game.
 */
public class BangView extends BWindow
    implements PlaceView
{
    /** Displays our board. */
    public BangBoardView view;

    /** Our chat display. */
    public ChatView chat;

    /** Creates the main panel and its sub-interfaces. */
    public BangView (BangContext ctx, BangController ctrl)
    {
        super(ctx.getLookAndFeel(), new BorderLayout());

        _ctx = ctx;
        _ctrl = ctrl;

        // create our various displays
        view = new BangBoardView(ctx, ctrl);
        _chatwin = new BWindow(ctx.getLookAndFeel(), new BorderLayout()) {
            // we never want the chat window to accept clicks
            public BComponent getHitComponent (int mx, int my) {
                return null;
            }
        };
        chat = new ChatView(_ctx, _ctx.getChatDirector()) {
            public void wasAdded () {
                super.wasAdded();
                _text.getBackground().getNode().setForceCull(true);
                _input.getBackground().getNode().setForceCull(true);
            }
            public void requestFocus () {
                super.requestFocus();
                _input.getBackground().getNode().setForceCull(false);
            }
            protected boolean handleInput (String text) {
                boolean chatted = super.handleInput(text);
                // relinquish the focus when we're done chatting
                if (chatted) {
                    _ctx.getInputDispatcher().requestFocus(null);
                    _input.getBackground().getNode().setForceCull(true);
                }
                return chatted;
            }
        };
        _chatwin.add(chat, BorderLayout.CENTER);
    }

    /** Called by the controller when the big shot and card selection
     * phase starts. */
    public void selectionPhase (BangObject bangobj, BangConfig cfg, int pidx)
    {
        // tell the board view to start the game so that we can see the
        // board while we're buying pieces
        view.startGame(bangobj, cfg, pidx);

        // add the selection view to the display
        _oview = new SelectionView(_ctx, cfg, bangobj, pidx);
        _ctx.getInputDispatcher().addWindow(_oview);
        _oview.pack();
        _oview.center();
    }

    /** Called by the controller when the buying phase starts. */
    public void buyingPhase (BangObject bangobj, BangConfig cfg, int pidx)
    {
        // remove the selection view from the display
        _ctx.getInputDispatcher().removeWindow(_oview);
        _oview = null;

        // add the purchase view to the display
        _oview = new PurchaseView(_ctx, cfg, bangobj, pidx);
        _ctx.getInputDispatcher().addWindow(_oview);
        _oview.pack();
        _oview.center();
    }

    /** Called by the controller when the game starts. */
    public void startGame (BangObject bangobj, BangConfig cfg, int pidx)
    {
        // remove the purchase (or in test mode, selection) view
        if (_oview != null) {
            _ctx.getInputDispatcher().removeWindow(_oview);
            _oview = null;
        }
    }

    /** Called by the controller when the game ends. */
    public void endGame ()
    {
        view.endGame();
    }

    // documentation inherited from interface
    public void willEnterPlace (PlaceObject plobj)
    {
        BangObject bangobj = (BangObject)plobj;

        // add the main bang view
        _ctx.getGeometry().attachChild(view.getNode());

        // add our chat display
        int width = _ctx.getDisplay().getWidth();
        _chatwin.setBounds(10, 20, width-20, 100);
        _ctx.getInputDispatcher().addWindow(_chatwin);

        // create and position our player status displays
        int pcount = bangobj.players.length;
        _pstatus = new BDecoratedWindow(_ctx.getLookAndFeel(), null);
        _pstatus.setLayoutManager(GroupLayout.makeHStretch());
        _pstatus.setBackground(
            new TintedBackground(5, 5, 5, 5, ColorRGBA.darkGray));
        for (int ii = 0; ii < pcount; ii++) {
            _pstatus.add(new PlayerStatusView(_ctx, bangobj, _ctrl, ii));
        }
        _ctx.getInputDispatcher().addWindow(_pstatus);
        _pstatus.setSize(_ctx.getDisplay().getWidth() - 20, 50);
        int height = _ctx.getDisplay().getHeight();
        _pstatus.setLocation(10, height - _pstatus.getHeight() - 10);

        chat.willEnterPlace(plobj);
    }

    // documentation inherited from interface
    public void didLeavePlace (PlaceObject plobj)
    {
        chat.didLeavePlace(plobj);

        // shut down the main game view
        view.shutdown();

        // remove our displays
        _ctx.getInputDispatcher().removeWindow(_pstatus);
        _ctx.getInputDispatcher().removeWindow(_chatwin);
        _ctx.getGeometry().detachChild(view.getNode());

        if (_oview != null) {
            _ctx.getInputDispatcher().removeWindow(_oview);
            _oview = null;
        }
    }

    /** Giver of life and context. */
    protected BangContext _ctx;

    /** Our game controller. */
    protected BangController _ctrl;

    /** Contain various onscreen displays. */
    protected BWindow _pstatus, _chatwin;

    /** Any window currently overlayed on the board. */
    protected BWindow _oview;
}
