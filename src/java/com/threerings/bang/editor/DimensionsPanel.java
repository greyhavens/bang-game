//
// $Id$

package com.threerings.bang.editor;

import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.VGroupLayout;

/**
 * A simple panel for entering integer width and height dimensions.
 */
public class DimensionsPanel extends JPanel
{
    public DimensionsPanel (EditorContext ctx)
    {
        super(new VGroupLayout());
        
        JPanel wpanel = new JPanel();
        wpanel.add(new JLabel(ctx.xlate("editor", "m.width")));
        wpanel.add(_width = new JFormattedTextField(
            NumberFormat.getIntegerInstance()));
        _width.setColumns(4);
        _width.setHorizontalAlignment(JFormattedTextField.RIGHT);
        add(wpanel);
        
        JPanel hpanel = new JPanel();
        hpanel.add(new JLabel(ctx.xlate("editor", "m.height")));
        hpanel.add(_height = new JFormattedTextField(
            NumberFormat.getIntegerInstance()));
        _height.setColumns(4);
        _height.setHorizontalAlignment(JFormattedTextField.RIGHT);
        add(hpanel);
    }
    
    /**
     * Returns the width entered by the user.
     */
    public int getWidthValue ()
    {
        return ((Number)_width.getValue()).intValue();
    }
    
    /**
     * Returns the height entered by the user.
     */
    public int getHeightValue ()
    {
        return ((Number)_height.getValue()).intValue();
    }
    
    /**
     * Sets the displayed dimensions.
     */
    public void setValues (int width, int height)
    {
        _width.setValue(new Integer(width));
        _height.setValue(new Integer(height));
    }
    
    /** The width and height fields. */
    protected JFormattedTextField _width, _height;
}
