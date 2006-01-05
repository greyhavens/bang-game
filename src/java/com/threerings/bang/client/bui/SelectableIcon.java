//
// $Id$

package com.threerings.bang.client.bui;

import com.jmex.bui.BToggleButton;

import com.threerings.bang.client.BangUI;

/**
 * Displays an icon image with text below it that can be selected when
 * clicked on with the mouse.
 */
public class SelectableIcon extends BToggleButton
{
    public SelectableIcon ()
    {
        super(null);
        setLookAndFeel(BangUI.iconLabelLNF);
    }

    @Override // documentation inherited
    protected void stateDidChange ()
    {
        super.stateDidChange();

        if (_parent instanceof IconPalette) {
            ((IconPalette)_parent).iconUpdated(this, _selected);
        }
    }
}
