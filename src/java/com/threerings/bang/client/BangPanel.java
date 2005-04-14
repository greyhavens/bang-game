//
// $Id$

package com.threerings.bang.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.Controller;
import com.samskivert.swing.ControllerProvider;
import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.ScrollBox;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.media.VirtualRangeModel;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.toybox.client.ChatPanel;
import com.threerings.toybox.data.ToyBoxGameConfig;
import com.threerings.toybox.util.ToyBoxContext;

import com.threerings.bang.data.BangObject;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Contains the primary user interface during the game.
 */
public class BangPanel extends JPanel
    implements ControllerProvider, PlaceView
{
    /** Displays our board. */
    public BangBoardView view;

    /** Creates the main panel and its sub-interfaces. */
    public BangPanel (ToyBoxContext ctx, BangController ctrl)
    {
        setLayout(new BorderLayout(0, 0));
        _ctx = ctx;
        _ctrl = ctrl;

	// give ourselves a wee bit of a border
	setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // create the board view and surprise display
        GroupLayout gl = new VGroupLayout(
            GroupLayout.STRETCH, GroupLayout.STRETCH, 5, GroupLayout.TOP);
        _gamePanel = new JPanel(gl);
        _gamePanel.add(view = new BangBoardView(ctx));
        gl = new HGroupLayout(GroupLayout.STRETCH, GroupLayout.STRETCH,
                              5, GroupLayout.LEFT);
        _spanel = new JPanel(gl);
        _gamePanel.add(_spanel, GroupLayout.FIXED);

        // create our side panel
        VGroupLayout sgl = new VGroupLayout(GroupLayout.STRETCH);
        sgl.setOffAxisPolicy(GroupLayout.STRETCH);
        sgl.setJustification(GroupLayout.TOP);
        _sidePanel = new JPanel(sgl);

        // add a big fat label because we love it!
        JLabel vlabel = new JLabel("Bang!");
        vlabel.setFont(new Font("Helvetica", Font.BOLD, 24));
        vlabel.setForeground(Color.black);
        _sidePanel.add(vlabel, GroupLayout.FIXED);

        // add a chat box
        ChatPanel chat = new ChatPanel(ctx);
        chat.removeSendButton();
        _sidePanel.add(chat);

        // add a box for scrolling around in our view
        _rangeModel = new VirtualRangeModel(view);
        _scrolly = new ScrollBox(_rangeModel.getHorizModel(),
                                 _rangeModel.getVertModel());
        _scrolly.setPreferredSize(new Dimension(100, 100));
        _scrolly.setBorder(BorderFactory.createLineBorder(Color.black));
        _sidePanel.add(_scrolly, VGroupLayout.FIXED);

        // add a "back" button
        JButton back = new JButton("Back to lobby");
        back.setActionCommand(BangController.BACK_TO_LOBBY);
        back.addActionListener(Controller.DISPATCHER);
        _sidePanel.add(back, VGroupLayout.FIXED);

        // add our side panel to the main display
        add(_sidePanel, BorderLayout.EAST);
    }

    /** Called by the controller when the buying phase starts. */
    public void buyingPhase (BangObject bangobj, ToyBoxGameConfig cfg, int pidx)
    {
        // remove the game view and add the purchase panel
        remove(view);
        _ppanel = new PurchasePanel(_ctx, cfg, bangobj, pidx);
        add(_ppanel, BorderLayout.CENTER);
        SwingUtil.refresh(this);
    }

    /** Called by the controller when the game starts. */
    public void startGame (BangObject bangobj, ToyBoxGameConfig cfg, int pidx)
    {
        // remove the purchase panel and add the game view
        remove(_ppanel);
        add(view, BorderLayout.CENTER);
        SwingUtil.refresh(this);

        // our view needs to know about the start of the game
        view.startGame(bangobj, cfg, pidx);

        // compute the size of the whole board and configure scrolling
        int width = bangobj.board.getWidth() * SQUARE,
            height = bangobj.board.getHeight() * SQUARE;
        if (width > view.getWidth() || height > view.getHeight()) {
            _rangeModel.setScrollableArea(-SQUARE, -SQUARE,
                                          width + 2*SQUARE, height + 2*SQUARE);
        } else {
            _scrolly.setVisible(false);
        }

        // add our surprise panels if necessary
        if (_spanel.getComponentCount() == 0) {
            for (int ii = 0; ii < bangobj.players.length; ii++) {
                SurprisePanel sp = new SurprisePanel(ii, pidx);
                _spanel.add(sp);
                // we need to fake a willEnterPlace() because they weren't
                // around when that happened
                sp.willEnterPlace(bangobj);
            }
            SwingUtil.refresh(_spanel);
        }
    }

    /** Called by the controller when the game ends. */
    public void endGame ()
    {
        view.endGame();
    }

    // documentation inherited from interface
    public Controller getController ()
    {
        return _ctrl;
    }

    // documentation inherited from interface
    public void willEnterPlace (PlaceObject plobj)
    {
        BangObject bangobj = (BangObject)plobj;

        // add score panels for each of our players
        for (int ii = 0; ii < bangobj.players.length; ii++) {
            _sidePanel.add(
                new ScorePanel(bangobj, ii), GroupLayout.FIXED, 1+ii);
        }
        SwingUtil.refresh(_sidePanel);
    }

    // documentation inherited from interface
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    /** Giver of life and context. */
    protected ToyBoxContext _ctx;

    /** Our game controller. */
    protected BangController _ctrl;

    /** Used to scroll around in our view. */
    protected VirtualRangeModel _rangeModel;

    /** Used to scroll around in our view. */
    protected ScrollBox _scrolly;

    /** The buying phase purchase panel. */
    protected PurchasePanel _ppanel;

    /** Contains the main game view. */
    protected JPanel _gamePanel;

    /** Contains all the stuff on the side. */
    protected JPanel _sidePanel;

    /** Displays our surprise panels. */
    protected JPanel _spanel;
}
