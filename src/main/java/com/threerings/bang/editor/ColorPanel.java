//
// $Id$

package com.threerings.bang.editor;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Allows the user to select a color.
 */
public class ColorPanel extends JPanel
{
    public ColorPanel (final EditorContext ctx, final String label)
    {
        add(new JLabel(ctx.xlate("editor", label)));
        add(_color = new JButton("    "));
        _color.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                Color chosen = JColorChooser.showDialog(ColorPanel.this,
                    ctx.xlate("editor", label), _color.getBackground());
                if (chosen != null) {
                    _color.setBackground(chosen);
                    fireChangeEvent();
                }
            }
        });
    }
    
    /**
     * Adds a listener for change events.
     */
    public void addChangeListener (ChangeListener cl)
    {
        _changeListeners.add(cl);
    }
    
    /**
     * Removes a listener for change events.
     */
    public void removeChangeListener (ChangeListener cl)
    {
        _changeListeners.remove(cl);
    }
    
    /**
     * Returns the selected RGB value.
     */
    public int getRGB ()
    {
        return _color.getBackground().getRGB();
    }
    
    /**
     * Sets the selected RGB value.
     */
    public void setRGB (int rgb)
    {
        _color.setBackground(new Color(rgb));
    }
    
    /**
     * Fires a change event to all listeners.
     */
    protected void fireChangeEvent ()
    {
        ChangeEvent e = new ChangeEvent(this);
        for (int i = 0, size = _changeListeners.size(); i < size; i++) {
            _changeListeners.get(i).stateChanged(e);
        }
    }
    
    /** The color button. */
    protected JButton _color;
    
    /** The list of change listeners. */    
    protected ArrayList<ChangeListener> _changeListeners =
        new ArrayList<ChangeListener>();   
}
