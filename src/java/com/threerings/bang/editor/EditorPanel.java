//
// $Id$

package com.threerings.bang.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import com.samskivert.swing.Controller;
import com.samskivert.swing.ControllerProvider;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.game.data.BangObject;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Contains the primary user interface for the editor mode.
 */
public class EditorPanel extends JPanel
    implements ControllerProvider, PlaceView
{
    /** Displays our board. */
    public EditorBoardView view;

    /** Displays board metadata. */
    public BoardInfo info;
    
    /** Allows user to select and configure tools. */
    public ToolPanel tools;

    /** Creates the main panel and its sub-interfaces. */
    public EditorPanel (EditorContext ctx, EditorController ctrl)
    {
        _ctx = ctx;
        _ctrl = ctrl;

        // give ourselves a wee bit of a border
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        MessageBundle msgs = ctx.getMessageManager().getBundle("editor");
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

        // add the various control panels
        sidePanel.add(info = new BoardInfo(ctx), VGroupLayout.FIXED);
        sidePanel.add(tools = new ToolPanel(ctx, this));

        // TODO: translate menu accelerators and short cuts
        JMenuBar menubar = _ctx.getFrame().getJMenuBar();

        JMenu file = new JMenu(msgs.get("m.menu_file"));
        file.setMnemonic(KeyEvent.VK_F);
        menubar.add(file);

        createMenuItem(file, msgs.get("m.menu_new"), KeyEvent.VK_N,
                       KeyEvent.VK_N, EditorController.NEW_BOARD);
        createMenuItem(file, msgs.get("m.menu_load"), KeyEvent.VK_O,
                       KeyEvent.VK_O, EditorController.LOAD_BOARD);
        createMenuItem(file, msgs.get("m.menu_save"), KeyEvent.VK_S,
                       KeyEvent.VK_S, EditorController.SAVE_BOARD);
        
        file.addSeparator();
        
        createMenuItem(file, msgs.get("m.menu_import_hf"), KeyEvent.VK_I,
                       KeyEvent.VK_I, EditorController.IMPORT_HEIGHTFIELD);
        createMenuItem(file, msgs.get("m.menu_export_hf"), KeyEvent.VK_E,
                       KeyEvent.VK_E, EditorController.EXPORT_HEIGHTFIELD);
        
        file.addSeparator();
        
        createMenuItem(file, msgs.get("m.menu_quit"), KeyEvent.VK_Q,
                       KeyEvent.VK_Q, EditorController.EXIT);
        
//         // add a "load" button
//         JButton load = new JButton(msgs.get("m.load_board"));
//         load.setActionCommand(EditorController.LOAD_BOARD);
//         load.addActionListener(ctrl);
//         sidePanel.add(load, VGroupLayout.FIXED);

//         // add a "save" button
//         JButton save = new JButton(msgs.get("m.save_board"));
//         save.setActionCommand(EditorController.SAVE_BOARD);
//         save.addActionListener(ctrl);
//         sidePanel.add(save, VGroupLayout.FIXED);

//         // add a "back" button
//         JButton back = new JButton(msgs.get("m.back_to_lobby"));
//         back.setActionCommand(EditorController.BACK_TO_LOBBY);
//         back.addActionListener(ctrl);
//         sidePanel.add(back, VGroupLayout.FIXED);

        JMenu edit = new JMenu(msgs.get("m.menu_edit"));
        edit.setMnemonic(KeyEvent.VK_E);
        menubar.add(edit);
        
        createMenuItem(edit, msgs.get("m.menu_light"), -1, KeyEvent.VK_L,
            EditorController.EDIT_LIGHT);
        createMenuItem(edit, msgs.get("m.menu_sky"), -1, KeyEvent.VK_S,
            EditorController.EDIT_SKY);
        createMenuItem(edit, msgs.get("m.menu_water"), -1, KeyEvent.VK_W,
            EditorController.EDIT_WATER);
        createMenuItem(edit, msgs.get("m.menu_board_size"), -1, KeyEvent.VK_B,
            EditorController.EDIT_BOARD_SIZE);
            
        JMenu view = new JMenu(msgs.get("m.menu_view"));
        view.setMnemonic(KeyEvent.VK_V);
        menubar.add(view);
        
        createCheckBoxMenuItem(view, msgs.get("m.menu_wireframe"),
            KeyEvent.VK_W, KeyEvent.VK_W, EditorController.TOGGLE_WIREFRAME,
            false);
        createCheckBoxMenuItem(view, msgs.get("m.menu_grid"),
            KeyEvent.VK_G, KeyEvent.VK_G, EditorController.TOGGLE_GRID,
            false);
        createCheckBoxMenuItem(view, msgs.get("m.menu_highlight"),
            KeyEvent.VK_H, KeyEvent.VK_H, EditorController.TOGGLE_HIGHLIGHTS,
            false);
        createMenuItem(view, msgs.get("m.menu_shadows"), -1, KeyEvent.VK_S,
            EditorController.GENERATE_SHADOWS);
            
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
        _ctx.getRootNode().pushDefaultEventTarget(view);
    }

    // documentation inherited from interface
    public void didLeavePlace (PlaceObject plobj)
    {
        _ctx.getGeometry().detachChild(view.getNode());
        _ctx.getRootNode().popDefaultEventTarget(view);
    }

    @Override // documentation inherited
    public Dimension getPreferredSize ()
    {
        Dimension d = super.getPreferredSize();
        d.width = 200;
        return d;
    }

    protected void createMenuItem (JMenu menu, String label, int accelerator,
                                   int mnemonic, String command)
    {
        JMenuItem item = new JMenuItem(label);
        if (accelerator != -1) {
            item.setAccelerator(KeyStroke.getKeyStroke(accelerator,
                ActionEvent.CTRL_MASK));
        }
        item.setMnemonic(mnemonic);
        item.setActionCommand(command);
        item.addActionListener(_ctrl);
        menu.add(item);
    }

    protected void createCheckBoxMenuItem (JMenu menu, String label,
        int accelerator, int mnemonic, String command, boolean selected)
    {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label);
        item.setAccelerator(KeyStroke.getKeyStroke(accelerator,
            ActionEvent.CTRL_MASK));
        item.setMnemonic(mnemonic);
        item.setActionCommand(command);
        item.addActionListener(_ctrl);
        item.setSelected(selected);
        menu.add(item);
    }
    
    /** Giver of life and context. */
    protected EditorContext _ctx;

    /** Our game controller. */
    protected EditorController _ctrl;
}
