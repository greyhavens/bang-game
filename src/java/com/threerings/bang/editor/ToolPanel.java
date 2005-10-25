//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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
    public ToolPanel (EditorContext ctx, EditorPanel panel)
    {
        super(new BorderLayout());
        
        // add the chooser panel on top
        JPanel cpanel = new JPanel(new HGroupLayout(HGroupLayout.STRETCH));
        cpanel.add(new JLabel(ctx.xlate("editor", "m.tool")),
            HGroupLayout.FIXED);
        _tools = new JComboBox();
        CameraDolly dolly = new CameraDolly(ctx, panel);
        _tools.addItem(dolly);
        _tools.addItem(new PiecePlacer(ctx, panel));
        _tools.addItem(new HeightfieldBrush(ctx, panel));
        _tools.addItem(new TerrainBrush(ctx, panel));
        _tools.addItemListener(this);
        cpanel.add(_tools);
        add(cpanel, BorderLayout.NORTH);
        
        // and the tool options below
        dolly.activate();
        add(_scroll = new JScrollPane(dolly.getOptions()),
            BorderLayout.CENTER);
        _scroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(6, 0, 0, 0), _scroll.getBorder()));
        SwingUtil.refresh(this);
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
    
    protected JComboBox _tools;
    protected JScrollPane _scroll;
}
