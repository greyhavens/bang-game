//
// $Id$

package com.threerings.bang.client.bui;

import com.jme.renderer.ColorRGBA;

import com.jme.bui.BIcon;
import com.jme.bui.BLookAndFeel;
import com.jme.bui.BToggleButton;
import com.jme.bui.background.BBackground;
import com.jme.bui.background.BlankBackground;
import com.jme.bui.border.BBorder;
import com.jme.bui.border.CompoundBorder;
import com.jme.bui.border.EmptyBorder;
import com.jme.bui.border.LineBorder;

/**
 * Displays an icon image with text below it that can be selected when
 * clicked on with the mouse.
 */
public class SelectableIcon extends BToggleButton
{
    public SelectableIcon (BIcon icon, String text)
    {
        super(text, "");
        setBorder(_unborder);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        // set up our custom look and feel
        if (_lnf == null) {
            _lnf = new SILookAndFeel(getLookAndFeel());
        }
        super.wasAdded();
    }

    @Override // documentation inherited
    protected void stateDidChange ()
    {
        super.stateDidChange();

        // configure our border
        setBorder(_selected ? _selborder : _unborder);
    }

    protected static class SILookAndFeel extends BLookAndFeel
    {
        public SILookAndFeel (BLookAndFeel parent)
        {
            super(parent);
        }

        public BBackground createButtonBack (int state)
        {
            return new BlankBackground();
        }
    }

    protected BBorder _selborder = new CompoundBorder(
        new LineBorder(ColorRGBA.black), new EmptyBorder(3, 3, 3, 3));
    protected BBorder _unborder = new EmptyBorder(4, 4, 4, 4);
}
