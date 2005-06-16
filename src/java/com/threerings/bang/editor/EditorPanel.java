//
// $Id$

package com.threerings.bang.editor;

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
import com.samskivert.swing.VGroupLayout;

import com.threerings.jme.JmeCanvasApp;
import com.threerings.media.SafeScrollPane;
import com.threerings.util.MessageBundle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Contains the primary user interface for the editor mode.
 */
public class EditorPanel extends JPanel
    implements ControllerProvider, PlaceView
{
    /** Displays our board. */
    public EditorBoardView view;

    /** Allows the selection of terrain. */
    public TerrainSelector terrain;

    /** Creates the main panel and its sub-interfaces. */
    public EditorPanel (BangContext ctx, EditorController ctrl)
    {
        _ctx = ctx;
        _ctrl = ctrl;

	// give ourselves a wee bit of a border
	setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        MessageBundle msgs = ctx.getMessageManager().getBundle(
            BangCodes.BANG_MSGS);
	HGroupLayout gl = new HGroupLayout(HGroupLayout.STRETCH);
	gl.setOffAxisPolicy(HGroupLayout.STRETCH);
	setLayout(gl);

        // create the board view
        view = new EditorBoardView(ctx, this);

        // create our side panel
        VGroupLayout sgl = new VGroupLayout(VGroupLayout.STRETCH);
        sgl.setOffAxisPolicy(VGroupLayout.STRETCH);
        sgl.setJustification(VGroupLayout.TOP);
        JPanel sidePanel = new JPanel(sgl);

        JLabel vlabel = new JLabel(msgs.get("m.editor_title"));
        vlabel.setFont(new Font("Helvetica", Font.BOLD, 24));
        vlabel.setForeground(Color.black);
        sidePanel.add(vlabel, VGroupLayout.FIXED);

        // add the terrain selector and piece creator
        sidePanel.add(terrain = new TerrainSelector(ctx), VGroupLayout.FIXED);
        PieceCreator pc = new PieceCreator(ctx);
        pc.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        sidePanel.add(new SafeScrollPane(pc));

        // add a "load" button
        JButton load = new JButton(msgs.get("m.load_board"));
        load.setActionCommand(EditorController.LOAD_BOARD);
        load.addActionListener(Controller.DISPATCHER);
        sidePanel.add(load, VGroupLayout.FIXED);

        // add a "save" button
        JButton save = new JButton(msgs.get("m.save_board"));
        save.setActionCommand(EditorController.SAVE_BOARD);
        save.addActionListener(Controller.DISPATCHER);
        sidePanel.add(save, VGroupLayout.FIXED);

        // add a "back" button
        JButton back = new JButton(msgs.get("m.back_to_lobby"));
        back.setActionCommand(EditorController.BACK_TO_LOBBY);
        back.addActionListener(Controller.DISPATCHER);
        sidePanel.add(back, VGroupLayout.FIXED);

        // add our side panel to the main display
        add(sidePanel, HGroupLayout.FIXED);
    }

    /** Called by the controller when the game starts. */
    public void startGame (BangObject bangobj, EditorConfig cfg)
    {
        // our view needs to know about the start of the game
        view.startGame(bangobj, cfg, 0);
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
        // add the main bang view
        _ctx.getGeometry().attachChild(view.getNode());
    }

    // documentation inherited from interface
    public void didLeavePlace (PlaceObject plobj)
    {
        _ctx.getGeometry().detachChild(view.getNode());
    }

    /** Giver of life and context. */
    protected BangContext _ctx;

    /** Our game controller. */
    protected EditorController _ctrl;
}
