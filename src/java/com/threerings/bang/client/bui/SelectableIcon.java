//
// $Id$

package com.threerings.bang.client.bui;

import com.jmex.bui.BToggleButton;

/**
 * Displays an icon image with text below it that can be selected when
 * clicked on with the mouse.
 */
public class SelectableIcon extends BToggleButton
{
    public SelectableIcon ()
    {
        super(null);
        setStyleClass("icon_label");
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
