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

    protected void setPalette (IconPalette palette)
    {
        _palette = palette;
    }

    @Override // documentation inherited
    protected void stateDidChange ()
    {
        super.stateDidChange();

        if (_palette != null) {
            _palette.iconUpdated(this, _selected);
        }
    }

    protected IconPalette _palette;
}
