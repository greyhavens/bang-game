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

    // documentation inherited
    protected void fireAction (long when, int modifiers)
    {
        super.fireAction(when, modifiers);

        // play a sound when an icon is selected (but only if we're going to remain selected; if
        // the palette we're in does not allow selection, we'll be immediately deselected again)
        if (_selected && _palette != null && _palette.getSelectable() > 0) {
            BangUI.play(BangUI.FeedbackSound.ITEM_SELECTED);
        }
    }

    @Override // from BComponent
    protected boolean changeCursor ()
    {
        return super.changeCursor() && (_palette == null || _palette.getSelectable() > 0);
    }

    protected IconPalette _palette;
}
