//
// $Id$

package com.threerings.bang.client;

import com.jme.bui.BLookAndFeel;
import com.jme.bui.BWindow;
import com.jme.bui.layout.BorderLayout;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.jme.chat.ChatView;

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

        // create a top-level window to contain our chat display
        BWindow chatwin = new BWindow(ctx.getLookAndFeel(), new BorderLayout());
        _chat = new ChatView(_ctx, _ctx.getChatDirector());
        chatwin.add(_chat, BorderLayout.CENTER);

        int width = _ctx.getDisplay().getWidth();
        chatwin.setBounds(10, 20, width-20, 100);
        _ctx.getInputDispatcher().addWindow(chatwin);
        _ctx.getInterface().attachChild(chatwin);

        // create our board view and add it to the display
        view = new BangBoardView(ctx, ctrl);
        _ctx.getGeometry().attachChild(view);

//         // create the board view and surprise display
//         GroupLayout gl = new VGroupLayout(
//             GroupLayout.STRETCH, GroupLayout.STRETCH, 5, GroupLayout.TOP);
//         _gamePanel = new JPanel(gl);
//         _gamePanel.add(view = new BangBoardView(ctx));
//         gl = new HGroupLayout(GroupLayout.STRETCH, GroupLayout.STRETCH,
//                               5, GroupLayout.LEFT);
//         _spanel = new JPanel(gl);
//         _gamePanel.add(_spanel, GroupLayout.FIXED);

//         // create our side panel
//         VGroupLayout sgl = new VGroupLayout(GroupLayout.STRETCH);
//         sgl.setOffAxisPolicy(GroupLayout.STRETCH);
//         sgl.setJustification(GroupLayout.TOP);
//         _sidePanel = new JPanel(sgl);
//         _sidePanel.setPreferredSize(new Dimension(250, 10));

//         // add a big fat label because we love it!
//         JLabel vlabel = new JLabel("Bang!");
//         vlabel.setFont(new Font("Helvetica", Font.BOLD, 24));
//         vlabel.setForeground(Color.black);
//         _sidePanel.add(vlabel, GroupLayout.FIXED);

//         // add a chat box
//         ChatPanel chat = new ChatPanel(ctx);
//         chat.removeSendButton();
//         _sidePanel.add(chat);

//         // add a "back" button
//         JButton back = new JButton("Back to lobby");
//         back.setActionCommand(BangController.BACK_TO_LOBBY);
//         back.addActionListener(Controller.DISPATCHER);
//         _sidePanel.add(back, VGroupLayout.FIXED);

//         // add our side panel to the main display
//         add(_sidePanel, BorderLayout.EAST);
    }

    /** Called by the controller when the buying phase starts. */
    public void buyingPhase (BangObject bangobj, BangConfig cfg, int pidx)
    {
        // add the purchase view to the display
        _pview = new PurchaseView(_ctx, cfg, bangobj, pidx);
        _ctx.getInputDispatcher().addWindow(_pview);
        _ctx.getInterface().attachChild(_pview);
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
        _ctx.getInterface().detachChild(_pview);
        _ctx.getInputDispatcher().removeWindow(_pview);
        _pview = null;

        // our view needs to know about the start of the game
        view.startGame(bangobj, cfg, pidx);

//         // add our surprise panels if necessary
//         if (_spanel.getComponentCount() == 0) {
//             for (int ii = 0; ii < bangobj.players.length; ii++) {
//                 SurprisePanel sp = new SurprisePanel(ii, pidx);
//                 _spanel.add(sp);
//                 // we need to fake a willEnterPlace() because they weren't
//                 // around when that happened
//                 sp.willEnterPlace(bangobj);
//             }
//         }
//         SwingUtil.refresh(this);
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

//         // add score panels for each of our players
//         for (int ii = 0; ii < bangobj.players.length; ii++) {
//             _sidePanel.add(
//                 new ScorePanel(bangobj, ii), GroupLayout.FIXED, 1+ii);
//         }
//         SwingUtil.refresh(_sidePanel);

        _chat.willEnterPlace(plobj);
    }

    // documentation inherited from interface
    public void didLeavePlace (PlaceObject plobj)
    {
        _chat.didLeavePlace(plobj);
    }

    /** Giver of life and context. */
    protected BangContext _ctx;

    /** Our game controller. */
    protected BangController _ctrl;

    /** Displays chat. */
    protected ChatView _chat;

    /** The buying phase purchase view. */
    protected PurchaseView _pview;

//     /** Contains the main game view. */
//     protected JPanel _gamePanel;

//     /** Contains all the stuff on the side. */
//     protected JPanel _sidePanel;

//     /** Displays our surprise panels. */
//     protected JPanel _spanel;
}
