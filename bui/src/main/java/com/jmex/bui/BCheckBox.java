//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import com.jmex.bui.icon.BIcon;

/**
 * Displays a label with a check-box button next to it.
 */
public class BCheckBox extends BToggleButton
{
    public BCheckBox (String label)
    {
        super(label);
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "checkbox";
    }

    // documentation inherited
    protected void configureStyle (BStyleSheet style)
    {
        super.configureStyle(style);

        for (int ii = 0; ii < getStateCount(); ii++) {
            _icons[ii] = style.getIcon(this, getStatePseudoClass(ii));
        }
        _label.setIcon(_icons[getState()]);
    }

    // documentation inherited
    protected void stateDidChange ()
    {
        super.stateDidChange();

        // configure our checkbox icon
        _label.setIcon(_icons[getState()]);
    }

    protected BIcon[] _icons = new BIcon[getStateCount()];
}
