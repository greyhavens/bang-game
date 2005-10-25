//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jme.math.Vector3f;

import com.jmex.bui.event.MouseEvent;

import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.util.BasicContext;

/**
 * Allows the user to raise, lower, and set parts of the heightfield.
 */
public class HeightfieldBrush extends EditorTool
{
    /** The name of this tool. */
    public static final String NAME = "heightfield_brush";
    
    public HeightfieldBrush (EditorContext ctx, EditorPanel panel)
    {
        super(ctx, panel);
        _cursor = panel.view.getTerrainNode().createCursor();
        _cursor.radius = DEFAULT_CURSOR_RADIUS;
    }
    
    // documentation inherited
    public String getName ()
    {
        return NAME;
    }
    
    @Override // documentation inherited
    public void activate ()
    {
        super.activate();
        _panel.view.getNode().attachChild(_cursor);
    }
    
    @Override // documentation inherited
    public void deactivate ()
    {
        super.deactivate();
        _panel.view.getNode().detachChild(_cursor);
    }
    
    @Override // documentation inherited
    public void mousePressed (MouseEvent e)
    {
        _lastPressed = e.getButton();
        byte value = ((Byte)_hbopts.value.getValue()).byteValue();
        _panel.view.paintHeightfield(_cursor.x, _cursor.y, _cursor.radius,
            _lastPressed == MouseEvent.BUTTON2 ? -value : +value,
            _hbopts.mode.getSelectedIndex() == ADD_VALUE);
    }
    
    @Override // documentation inherited
    public void mouseMoved (MouseEvent e)
    {
        Vector3f ground = _panel.view.getGroundIntersect(e, null);
        _cursor.setPosition(ground.x, ground.y);
    }
    
    @Override // documentation inherited
    public void mouseDragged (MouseEvent e)
    {
        mouseMoved(e);
        byte value = ((Byte)_hbopts.value.getValue()).byteValue();
        _panel.view.paintHeightfield(_cursor.x, _cursor.y, _cursor.radius,
            _lastPressed == MouseEvent.BUTTON2 ? -value : +value,
            _hbopts.mode.getSelectedIndex() == ADD_VALUE);
    }
    
    @Override // documentation inherited
    public void mouseWheeled (MouseEvent e)
    {
        _cursor.setRadius(Math.min(Math.max(_cursor.radius + e.getDelta(),
            MIN_CURSOR_RADIUS), MAX_CURSOR_RADIUS));
        _hbopts.sizer.setValue((int)_cursor.radius);
    }
    
    // documentation inherited
    protected JPanel createOptions ()
    {
        return (_hbopts = new HeightfieldBrushOptions());
    }
    
    /** The options for this panel. */
    protected class HeightfieldBrushOptions extends JPanel
        implements ActionListener, ChangeListener
    {
        public JSlider sizer;
        public JComboBox mode;
        public JFormattedTextField value;
        public JButton noise, generate;
        
        public HeightfieldBrushOptions ()
        {
            super(new VGroupLayout(VGroupLayout.NONE, VGroupLayout.TOP));
            setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            
            JPanel mpanel = new JPanel();
            mpanel.add(new JLabel(_msgs.get("m.mode")));
            mpanel.add(mode = new JComboBox(new Object[] {
                _msgs.get("m.add_value"), _msgs.get("m.set_value") }));
            add(mpanel);
            
            JPanel spanel = new JPanel();
            spanel.add(new JLabel(_msgs.get("m.brush_size")));
            spanel.add(sizer = new JSlider(MIN_CURSOR_RADIUS,
                MAX_CURSOR_RADIUS, DEFAULT_CURSOR_RADIUS));
            sizer.addChangeListener(this);
            sizer.setPreferredSize(new Dimension(70,
                sizer.getPreferredSize().height));
            add(spanel);
            
            JPanel vpanel = new JPanel();
            vpanel.add(new JLabel(_msgs.get("m.value")));
            vpanel.add(value = new JFormattedTextField(
                new JFormattedTextField.AbstractFormatter() {
                    public Object stringToValue (String text)
                        throws ParseException {
                        try {
                            return new Byte(text);
                            
                        } catch (NumberFormatException nfe) {
                            throw new ParseException(text, 0);
                        }
                    }
                    public String valueToString (Object value) {
                        return (value == null) ? "0" :
                            ((Byte)value).toString();
                    }
                }));
            value.setValue(new Byte(DEFAULT_VALUE));
            value.setColumns(4);
            value.setHorizontalAlignment(JFormattedTextField.RIGHT);
            add(vpanel);
            
            noise = new JButton(_msgs.get("b.add_noise"));
            noise.addActionListener(this);
            add(noise);
            
            generate = new JButton(_msgs.get("m.generate_fractal"));
            generate.addActionListener(this);
            add(generate);
        }
        
        public void stateChanged (ChangeEvent e)
        {
            _cursor.setRadius(sizer.getValue());
        }
        
        public void actionPerformed (ActionEvent e)
        {
            if (e.getSource() == noise) {
                _panel.view.addHeightfieldNoise(
                    ((Byte)value.getValue()).byteValue());
                    
            } else { // e.getSource() == generate
                new GenerateDialog().setVisible(true);
            }
        }
    }
    
    /** A dialog for generating terrain using JME's utility classes. */
    protected class GenerateDialog extends JDialog
        implements ActionListener
    {
        public JTabbedPane tabs;
        public JButton generate, dismiss;
        
        public GenerateDialog ()
        {
            super(_ctx.getFrame(), _msgs.get("m.generate_fractal"), true);
            
            tabs = new JTabbedPane();
            tabs.setPreferredSize(new Dimension(400, 200));
            getContentPane().add(tabs, BorderLayout.CENTER);
            
            JPanel mdpanel = new JPanel();
            JPanel rpanel = new JPanel();
            rpanel.add(new JLabel(_msgs.get("m.roughness")));
            //rpanel.add(
            mdpanel.add(rpanel);
            tabs.addTab(_msgs.get("m.midpoint_displacement"), mdpanel);
            
            JPanel buttons = new JPanel();
            buttons.add(generate = new JButton(_msgs.get("b.generate")));
            generate.addActionListener(this);
            buttons.add(dismiss = new JButton(_msgs.get("b.dismiss")));
            dismiss.addActionListener(this);
            getContentPane().add(buttons, BorderLayout.SOUTH);
            
            setBounds(100, 100, 500, 300);
            setResizable(false);
            setLocationRelativeTo(_ctx.getFrame());
        }
        
        public void actionPerformed (ActionEvent e)
        {
            if (e.getSource() == generate) {
                _panel.view.generateMidpointDisplacement(1.0f);
                
            } else { // e.getSource() == dismiss
                setVisible(false);
            }
        }
    }
    
    /** The heightfield cursor. */
    protected TerrainNode.Cursor _cursor;
    
    /** The casted options panel. */
    protected HeightfieldBrushOptions _hbopts;
    
    /** The last mouse button pressed. */
    protected int _lastPressed;
    
    /** The add value heightfield mode. */
    protected static final int ADD_VALUE = 0;
    
    /** The set value heightfield mode. */
    protected static final int SET_VALUE = 1;
    
    /** The default value. */
    protected static final byte DEFAULT_VALUE = 5;
    
    /** The minimum cursor radius. */
    protected static final int MIN_CURSOR_RADIUS = 1;
    
    /** The default cursor radius. */
    protected static final int DEFAULT_CURSOR_RADIUS = 5;
    
    /** The maximum cursor radius. */
    protected static final int MAX_CURSOR_RADIUS = 50;
}
