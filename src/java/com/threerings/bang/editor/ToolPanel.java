//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseWheelListener;
import com.jmex.bui.event.MouseAdapter;
import com.samskivert.swing.Controller;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.bang.game.data.Terrain;

/**
 * Displays a panel that allows the user to select and configure the active
 * tool.
 */
public class ToolPanel extends JPanel
    implements ItemListener
{
    public CameraDolly cameraDolly;
    
    public ToolPanel (EditorContext ctx, EditorPanel panel)
    {
        super(new BorderLayout());
        
        // add the chooser panel on top
        JPanel cpanel = new JPanel(new HGroupLayout(HGroupLayout.STRETCH));
        cpanel.add(new JLabel(ctx.xlate("editor", "m.tool")),
            HGroupLayout.FIXED);
        _tools = new JComboBox();
        cameraDolly = new CameraDolly(ctx, panel);
        _tools.addItem(cameraDolly);
        _tools.addItem(new PiecePlacer(ctx, panel));
        _tools.addItem(new ViewpointEditor(ctx, panel));
        _tools.addItem(new TrackLayer(ctx, panel));
        _tools.addItem(new HeightfieldBrush(ctx, panel));
        _tools.addItem(new TerrainBrush(ctx, panel));
        _tools.addItemListener(this);
        cpanel.add(_tools);
        add(cpanel, BorderLayout.NORTH);
        
        // add actions to select tools using ctrl-1+
        addSelectAction(panel, KeyEvent.VK_1, 0);
        addSelectAction(panel, KeyEvent.VK_2, 1);
        addSelectAction(panel, KeyEvent.VK_3, 2);
        addSelectAction(panel, KeyEvent.VK_4, 3);
        addSelectAction(panel, KeyEvent.VK_5, 4);
        addSelectAction(panel, KeyEvent.VK_6, 5);
        
        // and the tool options below
        add(_scroll = new JScrollPane(cameraDolly.getOptions()),
            BorderLayout.CENTER);
        _scroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(6, 0, 0, 0), _scroll.getBorder()));
        SwingUtil.refresh(this);

        // add our event dispatcher
        panel.view.addListener(_dispatcher);
    }
    
    /**
     * Returns a reference to the active tool.
     */
    public EditorTool getActiveTool ()
    {
        return (EditorTool)_tools.getSelectedItem();
    }
    
    /**
     * Selects a tool by name.
     */
    public void selectTool (String name)
    {
        for (int i = 0, c = _tools.getItemCount(); i < c; i++) {
            EditorTool tool = (EditorTool)_tools.getItemAt(i);
            if (tool.getName().equals(name)) {
                _tools.setSelectedIndex(i);
                return;
            }
        }
    }
    
    // inherited from interface ItemListener
    public void itemStateChanged (ItemEvent ie)
    {
        EditorTool tool = (EditorTool)ie.getItem();
        if (ie.getStateChange() == ItemEvent.SELECTED) {
            _scroll.setViewportView(tool.getOptions());
            SwingUtil.refresh(this);
            tool.activate();
            
        } else if (ie.getStateChange() == ItemEvent.DESELECTED) {
            tool.deactivate();
        }
    }
    
    /**
     * Adds a binding to ctrl-keyCode that selected the tool at the given
     * index.
     */
    protected void addSelectAction (JPanel panel, int keyCode, final int index)
    {
        String key = "select_tool_" + index;
        panel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(keyCode, KeyEvent.CTRL_DOWN_MASK), key);
        panel.getActionMap().put(key, new AbstractAction() {
            public void actionPerformed (ActionEvent e) {
                _tools.setSelectedIndex(index);
            }
        });
    }

    protected EditorTool getTool (MouseEvent e)
    {
        if ((e.getModifiers() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
            return cameraDolly;
        } else {
            return getActiveTool();
        }
    }

    protected class EventDispatcher extends MouseAdapter
        implements MouseWheelListener
    {
        public void mousePressed (MouseEvent e) {
            getTool(e).mousePressed(e);
        }
        public void mouseReleased (MouseEvent e) {
            getTool(e).mouseReleased(e);
        }
        public void mouseMoved (MouseEvent e) {
            getTool(e).mouseMoved(e);
        }
        public void mouseDragged (MouseEvent e) {
            getTool(e).mouseDragged(e);
        }
        public void mouseWheeled (MouseEvent e) {
            getTool(e).mouseWheeled(e);
        }
    }

    protected JComboBox _tools;
    protected JScrollPane _scroll;
    protected EventDispatcher _dispatcher = new EventDispatcher();
}
