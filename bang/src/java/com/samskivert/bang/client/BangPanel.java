//
// $Id$

package com.samskivert.bang.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.bang.data.BangObject;
import com.samskivert.swing.Controller;
import com.samskivert.swing.ControllerProvider;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.ScrollBox;
import com.samskivert.swing.VGroupLayout;

import com.threerings.media.VirtualRangeModel;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.toybox.client.ChatPanel;
import com.threerings.toybox.util.ToyBoxContext;

import static com.samskivert.bang.client.BangMetrics.*;

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
        _ctrl = ctrl;

	// give ourselves a wee bit of a border
	setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	HGroupLayout gl = new HGroupLayout(HGroupLayout.STRETCH);
	gl.setOffAxisPolicy(HGroupLayout.STRETCH);
	setLayout(gl);

        // create the board view
        add(view = new BangBoardView(ctx));

        // create our side panel
        VGroupLayout sgl = new VGroupLayout(VGroupLayout.STRETCH);
        sgl.setOffAxisPolicy(VGroupLayout.STRETCH);
        sgl.setJustification(VGroupLayout.TOP);
        JPanel sidePanel = new JPanel(sgl);

        // add a big fat label because we love it!
        JLabel vlabel = new JLabel("Bang!");
        vlabel.setFont(new Font("Helvetica", Font.BOLD, 24));
        vlabel.setForeground(Color.black);
        sidePanel.add(vlabel, VGroupLayout.FIXED);

        // add a chat box
        ChatPanel chat = new ChatPanel(ctx);
        chat.removeSendButton();
        sidePanel.add(chat);

        // add a box for scrolling around in our view
        _rangeModel = new VirtualRangeModel(view);
        _scrolly = new ScrollBox(_rangeModel.getHorizModel(),
                                 _rangeModel.getVertModel());
        _scrolly.setPreferredSize(new Dimension(100, 100));
        _scrolly.setBorder(BorderFactory.createLineBorder(Color.black));
        sidePanel.add(_scrolly, VGroupLayout.FIXED);

        // add a "back" button
        JButton back = new JButton("Back to lobby");
        back.setActionCommand(BangController.BACK_TO_LOBBY);
        back.addActionListener(Controller.DISPATCHER);
        sidePanel.add(back, VGroupLayout.FIXED);

        // add our side panel to the main display
        add(sidePanel, HGroupLayout.FIXED);
    }

    /** Called by the controller when the game starts. */
    public void startGame (BangObject bangobj, int pidx)
    {
        // our view needs to know about the start of the game
        view.startGame(bangobj, pidx);

        // compute the size of the whole board and configure scrolling
        int width = bangobj.board.getWidth() * SQUARE,
            height = bangobj.board.getHeight() * SQUARE;
        if (width > view.getWidth() || height > view.getHeight()) {
            _rangeModel.setScrollableArea(0, 0, width, height);
        } else {
            _scrolly.setVisible(false);
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
    }

    // documentation inherited from interface
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    /** Our game controller. */
    protected BangController _ctrl;

    /** Used to scroll around in our view. */
    protected VirtualRangeModel _rangeModel;

    /** Used to scroll around in our view. */
    protected ScrollBox _scrolly;
}
