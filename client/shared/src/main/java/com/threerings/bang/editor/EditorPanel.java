//
// $Id$

package com.threerings.bang.editor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.jme.system.DisplaySystem;
import com.jmex.bui.BWindow;
import com.jmex.bui.layout.BorderLayout;

import com.samskivert.swing.Controller;
import com.samskivert.swing.ControllerProvider;
import com.samskivert.swing.VGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.game.data.BangObject;

/**
 * Contains the primary user interface for the editor mode.
 */
public class EditorPanel extends JPanel
    implements ControllerProvider, PlaceView
{
    /** Displays our board. */
    public EditorBoardView view;

    /** Displays board metadata. */
    public BoardInfoPanel info;

    /** Allows user to select and configure tools. */
    public ToolPanel tools;

    /** The undo and redo menu items, which are enabled and disabled by the
     * controller. */
    public JMenuItem undo, redo;

    /** The recenter camera menu item, which is enabled and disabled by the
     * viewpoint editor. */
    public JMenuItem recenter;

    /** The prop menu items. */
    public JCheckBoxMenuItem[] propChecks;

    /** Creates the main panel and its sub-interfaces. */
    public EditorPanel (EditorContext ctx, EditorController ctrl)
    {
        _ctx = ctx;
        _ctrl = ctrl;

        // give ourselves a wee bit of a border
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        MessageBundle msgs = ctx.getMessageManager().getBundle("editor");
	    VGroupLayout gl = new VGroupLayout(VGroupLayout.STRETCH);
	    gl.setOffAxisPolicy(VGroupLayout.EQUALIZE);
	    gl.setJustification(VGroupLayout.TOP);
        setLayout(gl);

        // create the board view
        view = new EditorBoardView(ctx, this);

        JLabel vlabel = new JLabel(msgs.get("m.editor_title"));
        vlabel.setFont(new Font("Helvetica", Font.BOLD, 24));
        vlabel.setForeground(Color.black);
        add(vlabel, VGroupLayout.FIXED);

        // add the various control panels
        add(info = new BoardInfoPanel(ctx, this), VGroupLayout.FIXED);
        add(tools = new ToolPanel(ctx, this));

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

        JMenu edit = new JMenu(msgs.get("m.menu_edit"));
        edit.setMnemonic(KeyEvent.VK_E);
        menubar.add(edit);

        undo = createMenuItem(edit, msgs.get("m.menu_undo"), KeyEvent.VK_Z,
            KeyEvent.VK_U, EditorController.UNDO);
        redo = createMenuItem(edit, msgs.get("m.menu_redo"), KeyEvent.VK_Y,
            KeyEvent.VK_R, EditorController.REDO);
        undo.setEnabled(false);
        redo.setEnabled(false);
        edit.addSeparator();
        createMenuItem(edit, msgs.get("m.menu_light"), -1, KeyEvent.VK_L,
            EditorController.EDIT_LIGHT);
        createMenuItem(edit, msgs.get("m.menu_sky"), -1, KeyEvent.VK_S,
            EditorController.EDIT_SKY);
        createMenuItem(edit, msgs.get("m.menu_water"), -1, KeyEvent.VK_W,
            EditorController.EDIT_WATER);
        createMenuItem(edit, msgs.get("m.menu_environment"), -1, KeyEvent.VK_E,
            EditorController.EDIT_ENVIRONMENT);
        createMenuItem(edit, msgs.get("m.menu_board_props"), KeyEvent.VK_P,
            KeyEvent.VK_P, EditorController.EDIT_BOARD_PROPERTIES);
        edit.addSeparator();
        createMenuItem(edit, msgs.get("m.menu_generate_shadows"), -1,
            KeyEvent.VK_G, EditorController.GENERATE_SHADOWS);
        createMenuItem(edit, msgs.get("m.menu_clear_shadows"), -1,
            KeyEvent.VK_C, EditorController.CLEAR_SHADOWS);

        JMenu view = new JMenu(msgs.get("m.menu_view"));
        view.setMnemonic(KeyEvent.VK_V);
        menubar.add(view);

        createCheckBoxMenuItem(view, msgs.get("m.menu_wireframe"),
            KeyEvent.VK_W, KeyEvent.VK_W, EditorController.TOGGLE_WIREFRAME,
            false);
        createCheckBoxMenuItem(view, msgs.get("m.menu_bounds"),
            KeyEvent.VK_B, KeyEvent.VK_B, EditorController.TOGGLE_BOUNDS,
            false);
        createCheckBoxMenuItem(view, msgs.get("m.menu_grid"),
            KeyEvent.VK_G, KeyEvent.VK_G, EditorController.TOGGLE_GRID,
            true);
        createCheckBoxMenuItem(view, msgs.get("m.menu_highlight"),
            KeyEvent.VK_H, KeyEvent.VK_H, EditorController.TOGGLE_HIGHLIGHTS,
            false);
        createCheckBoxMenuItem(view, msgs.get("m.menu_markers"),
            KeyEvent.VK_M, KeyEvent.VK_M, EditorController.TOGGLE_MARKERS,
            true);

        view.addSeparator();
        recenter = createMenuItem(view, msgs.get("m.menu_recenter_camera"),
            KeyEvent.VK_R, KeyEvent.VK_R, EditorController.RECENTER_CAMERA);
    }

    /** Called by the controller when the "editing" game starts. */
    public void startEditing (BangObject bangobj, EditorConfig cfg)
    {
        view.prepareForRound(bangobj, cfg, 0);
    }

    /** Called by the controller when we're leaving the "editing" game. */
    public void endEditing ()
    {
        view.endRound();
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
        _vwin = new BWindow(_ctx.getStyleSheet(), new BorderLayout());
        _vwin.add(view, BorderLayout.CENTER);
        _ctx.getRootNode().addWindow(_vwin);
        DisplaySystem ds = DisplaySystem.getDisplaySystem();

        // resize the window with the canvas
        ((EditorApp)_ctx.getApp()).getCanvas().addComponentListener(
            new ComponentAdapter() {
                public void componentResized (ComponentEvent e) {
                    Component c = e.getComponent();
                    _vwin.setBounds(0, 0, c.getWidth(), c.getHeight());
                }
            }
        );

        _vwin.setBounds(0, 0, ds.getWidth(), ds.getHeight());
    }

    // documentation inherited from interface
    public void didLeavePlace (PlaceObject plobj)
    {
        _ctx.getRootNode().removeWindow(_vwin);
    }

    protected JMenuItem createMenuItem (
        JMenu menu, String label, int accelerator, int mnemonic,
        String command)
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
        return item;
    }

    protected JCheckBoxMenuItem createCheckBoxMenuItem (
            JMenu menu, String label, int accelerator, int mnemonic,
            String command, boolean selected)
    {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label);
        if (accelerator != -1) {
            item.setAccelerator(KeyStroke.getKeyStroke(accelerator,
                ActionEvent.CTRL_MASK));
        }
        if (mnemonic != -1) {
            item.setMnemonic(mnemonic);
        }
        item.setActionCommand(command);
        item.addActionListener(_ctrl);
        item.setSelected(selected);
        menu.add(item);
        return item;
    }

    /** Giver of life and context. */
    protected EditorContext _ctx;

    /** Our game controller. */
    protected EditorController _ctrl;

    /** A window that contains the editor view. */
    protected BWindow _vwin;
}
