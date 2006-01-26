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
    }

    @Override // documentation inherited
    protected void stateDidChange ()
    {
        super.stateDidChange();

        if (_parent != null && _parent.getParent() instanceof IconPalette) {
            ((IconPalette)_parent.getParent()).iconUpdated(this, _selected);
        }
    }
}
