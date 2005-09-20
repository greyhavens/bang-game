//
// $Id$

package com.threerings.bang.client.bui;

import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BIcon;
import com.jmex.bui.BLookAndFeel;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.background.BBackground;
import com.jmex.bui.background.BlankBackground;
import com.jmex.bui.border.BBorder;
import com.jmex.bui.border.CompoundBorder;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.border.LineBorder;

/**
 * Displays an icon image with text below it that can be selected when
 * clicked on with the mouse.
 */
public class SelectableIcon extends BToggleButton
{
    public SelectableIcon ()
    {
        super(null);
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

        if (_selected && _parent instanceof IconPalette) {
            ((IconPalette)_parent).iconSelected(this);
        }
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
