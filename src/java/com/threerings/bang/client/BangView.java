//
// $Id$

package com.threerings.bang.client;

import java.awt.Dimension;

import com.jme.bui.BLookAndFeel;
import com.jme.bui.BWindow;
import com.jme.bui.background.TintedBackground;
import com.jme.bui.layout.BorderLayout;
import com.jme.bui.layout.TableLayout;
import com.jme.renderer.ColorRGBA;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.jme.chat.ChatView;

import com.threerings.bang.client.sprite.UnitSprite;
import com.threerings.bang.data.BangConfig;
import com.threerings.bang.data.BangObject;
import com.threerings.bang.util.BangContext;

/**
 * Manages the primary user interface during the game.
 */
public class BangView
    implements PlaceView
{
    /** Displays our board. */
    public BangBoardView view;

    /** Creates the main panel and its sub-interfaces. */
    public BangView (BangContext ctx, BangController ctrl)
    {
        _ctx = ctx;
        _ctrl = ctrl;

        // create our various displays
        view = new BangBoardView(ctx, ctrl);
        _chatwin = new BWindow(ctx.getLookAndFeel(), new BorderLayout());
        _chat = new ChatView(_ctx, _ctx.getChatDirector());
        _chatwin.add(_chat, BorderLayout.CENTER);
    }

    /** Called by the controller when the buying phase starts. */
    public void buyingPhase (BangObject bangobj, BangConfig cfg, int pidx)
    {
        // set up the background according to which player we are
        ColorRGBA color = (pidx == -1) ?
            ColorRGBA.gray : UnitSprite.JPIECE_COLORS[pidx];
        _ctx.getRenderer().setBackgroundColor(color);

        // add the purchase view to the display
        _pview = new PurchaseView(_ctx, cfg, bangobj, pidx);
        _ctx.getInputDispatcher().addWindow(_pview);
        _pview.pack();
        int width = _ctx.getDisplay().getWidth();
        int height = _ctx.getDisplay().getHeight();
        _pview.setLocation((width - _pview.getWidth())/2,
                           (height - _pview.getHeight())/2);
    }

    /** Called by the controller when the game starts. */
    public void startGame (BangObject bangobj, BangConfig cfg, int pidx)
    {
        // remove the purchase view from the display
        _ctx.getInputDispatcher().removeWindow(_pview);
        _pview = null;

        // our view needs to know about the start of the game
        view.startGame(bangobj, cfg, pidx);
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
        _pstatus = new BWindow(
            _ctx.getLookAndFeel(),
            new TableLayout(pcount, 10, 10, TableLayout.STRETCH));
        _pstatus.setBackground(
            new TintedBackground(0, 0, 0, 0, new ColorRGBA(0, 0, 0, 0.5f)));
        for (int ii = 0; ii < pcount; ii++) {
            _pstatus.add(new PlayerStatusView(_ctx, bangobj, _ctrl, ii));
        }
        _ctx.getInputDispatcher().addWindow(_pstatus);
        _pstatus.setSize(_ctx.getDisplay().getWidth() - 20, 50);
        int height = _ctx.getDisplay().getHeight();
        _pstatus.setLocation(10, height - _pstatus.getHeight() - 10);

        _chat.willEnterPlace(plobj);
    }

    // documentation inherited from interface
    public void didLeavePlace (PlaceObject plobj)
    {
        _chat.didLeavePlace(plobj);

        // shut down the main game view
        view.shutdown();

        // remove our displays
        _ctx.getInputDispatcher().removeWindow(_pstatus);
        _ctx.getInputDispatcher().removeWindow(_chatwin);
        _ctx.getGeometry().detachChild(view.getNode());

        if (_pview != null) {
            _ctx.getInputDispatcher().removeWindow(_pview);
            _pview = null;
        }
    }

    /** Giver of life and context. */
    protected BangContext _ctx;

    /** Our game controller. */
    protected BangController _ctrl;

    /** Contain various onscreen displays. */
    protected BWindow _pstatus, _chatwin;

    /** Displays chat. */
    protected ChatView _chat;

    /** The buying phase purchase view. */
    protected PurchaseView _pview;
}
