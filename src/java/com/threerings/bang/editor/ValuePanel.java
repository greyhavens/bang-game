//
// $Id$

package com.threerings.bang.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.text.ParseException;

import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JFormattedTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;

/**
 * Combines a slider with a numeric entry field.
 */
public class ValuePanel extends JPanel
{
    public ValuePanel (String label, final int min, final int max, int value)
    {
        add(new JLabel(label));
        add(_slider = new JSlider(min, max, value));
        _slider.addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent e) {
                _field.setValue(Integer.valueOf(_slider.getValue()));
                fireChangeEvent();
            }
        });
        add(_field = new JFormattedTextField(new DefaultFormatter() {
            public Object stringToValue (String string)
                throws ParseException {
                Integer value = (Integer)super.stringToValue(string);
                if (value.intValue() < min || value.intValue() > max) {
                    throw new ParseException("out of bounds", 0);
                }
                return value;
            }
        }));
        _field.setColumns(4);
        _field.setHorizontalAlignment(JFormattedTextField.RIGHT);
        _field.addPropertyChangeListener("value",
            new PropertyChangeListener() {
            public void propertyChange (PropertyChangeEvent e) {
                _slider.setValue(((Integer)_field.getValue()).intValue());
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
     * Returns the value shown in this panel.
     */
    public int getValue ()
    {
        return _slider.getValue();
    }
    
    /**
     * Sets the value shown in this panel.
     */
    public void setValue (int value)
    {
        _slider.setValue(value);
        _field.setValue(Integer.valueOf(value));
    }
    
    /**
     * Checks whether the user is still adjusting the value.
     */
    public boolean getValueIsAdjusting ()
    {
        return _slider.getValueIsAdjusting();
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
    
    protected JSlider _slider;
    protected JFormattedTextField _field;
    
    /** The list of change listeners. */    
    protected ArrayList<ChangeListener> _changeListeners =
        new ArrayList<ChangeListener>();
}
