//
// $Id$

package com.threerings.bang.game.client;

import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BComponent;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BWindow;
import com.jmex.bui.background.BBackground;
import com.jmex.bui.background.TintedBackground;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

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
    public OverlayChatView chat;

    /** Creates the main panel and its sub-interfaces. */
    public BangView (BangContext ctx, BangController ctrl)
    {
        super(ctx.getLookAndFeel(), new BorderLayout());

        _ctx = ctx;
        _ctrl = ctrl;

        // create our various displays
        add(view = new BangBoardView(ctx, ctrl), BorderLayout.CENTER);
        chat = new OverlayChatView(ctx);
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
        _ctx.getRootNode().addWindow(_oview);
        _oview.pack();
        _oview.center();
    }

    /** Called by the controller when the buying phase starts. */
    public void buyingPhase (BangObject bangobj, BangConfig cfg, int pidx)
    {
        // remove the selection view from the display
        _ctx.getRootNode().removeWindow(_oview);
        _oview = null;

        // add the purchase view to the display
        _oview = new PurchaseView(_ctx, cfg, bangobj, pidx);
        _ctx.getRootNode().addWindow(_oview);
        _oview.pack();
        _oview.center();
    }

    /** Called by the controller when the game starts. */
    public void startGame (BangObject bangobj, BangConfig cfg, int pidx)
    {
        // remove the purchase (or in test mode, selection) view
        if (_oview != null) {
            _ctx.getRootNode().removeWindow(_oview);
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

        // add our chat display
        int width = _ctx.getDisplay().getWidth();
        _ctx.getRootNode().addWindow(chat);
        chat.pack();
        chat.setBounds(10, 20, width-20, chat.getHeight());

        // create and position our player status displays
        int pcount = bangobj.players.length;
        _pstatus = new BDecoratedWindow(_ctx.getLookAndFeel(), null);
        _pstatus.setLayoutManager(GroupLayout.makeHStretch());
        _pstatus.setBackground(
            new TintedBackground(ColorRGBA.darkGray, 5, 5, 5, 5));
        for (int ii = 0; ii < pcount; ii++) {
            _pstatus.add(new PlayerStatusView(_ctx, bangobj, _ctrl, ii));
        }
        _ctx.getRootNode().addWindow(_pstatus);
        _pstatus.setSize(_ctx.getDisplay().getWidth() - 20, 50);
        int height = _ctx.getDisplay().getHeight();
        _pstatus.setLocation(10, height - _pstatus.getHeight() - 10);

        chat.willEnterPlace(plobj);
    }

    // documentation inherited from interface
    public void didLeavePlace (PlaceObject plobj)
    {
        chat.didLeavePlace(plobj);

        // remove our displays
        _ctx.getRootNode().removeWindow(_pstatus);
        _ctx.getRootNode().removeWindow(chat);

        if (_oview != null) {
            _ctx.getRootNode().removeWindow(_oview);
            _oview = null;
        }
    }

    /** Giver of life and context. */
    protected BangContext _ctx;

    /** Our game controller. */
    protected BangController _ctrl;

    /** Contain various onscreen displays. */
    protected BWindow _pstatus;

    /** Any window currently overlayed on the board. */
    protected BWindow _oview;
}
