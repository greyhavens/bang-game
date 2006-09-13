//
// $Id$

package com.threerings.bang.client.util;

import com.jmex.bui.BComboBox;
import com.jmex.bui.BComponent;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.bang.client.BangPrefs;

import static com.threerings.bang.Log.log;

/**
 * Saves the configuration of a particular user interface component in a
 * persistent preference. <em>Note:</em> this class is not smart, and will not
 * work if, for example, a combo box has different contents depending on
 * context. It saves the up/down state of any toggle button and the selected
 * index of any combo box, so be sure that it makes sense for the setting
 * you're trying to preserve.
 */
public class StateSaver
    implements ActionListener
{
    public StateSaver (String prefName, BComponent component)
    {
        _prefName = prefName;
        _component = component;
        _component.addListener(this);

        // configure the component based on the current preference value
        if (component instanceof BToggleButton) {
            BToggleButton toggle = (BToggleButton)component;
            toggle.setSelected(
                BangPrefs.config.getValue(_prefName, toggle.isSelected()));

        } else if (component instanceof BComboBox) {
            BComboBox combo = (BComboBox)component;
            combo.selectItem(
                BangPrefs.config.getValue(_prefName, combo.getSelectedIndex()));

        } else {
            log.warning("Cannot store preferences for unknown component " +
                "type " + component + ".");
        }
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // save the current value
        if (_component instanceof BToggleButton) {
            BangPrefs.config.setValue(
                _prefName, ((BToggleButton)_component).isSelected());
        } else if (_component instanceof BComboBox) {
            BangPrefs.config.setValue(
                _prefName, ((BComboBox)_component).getSelectedIndex());
        }
    }

    protected String _prefName;
    protected BComponent _component;
}
