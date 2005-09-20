//
// $Id$

package com.threerings.bang.client.bui;

import com.jmex.bui.BContainer;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.layout.TableLayout;

/**
 * Displays a palette of icons that can be selected by the user.
 */
public class IconPalette extends BContainer
{
    /** Used to inform an external inspector that a particular icon has been
     * selected. */
    public interface Inspector
    {
        /** Called to indicate that the supplied icon has been selected in the
         * icon palette. */
        public void iconSelected (SelectableIcon icon);
    }

    /**
     * Creates an icon palette with the supplied (optional) inspector. Icons
     * may be added via {@link #add} but they must derive from {@link
     * SelectableIcon}.
     */
    public IconPalette (Inspector inspector)
    {
        super(new TableLayout(4, 5, 5));
        setBorder(new EmptyBorder(5, 5, 5, 5));
        _inspector = inspector;
    }

    /**
     * Returns the selected icon or null if none is selected.
     */
    public SelectableIcon getSelectedIcon ()
    {
        return _selection;
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        iconSelected(null);
    }

    protected void iconSelected (SelectableIcon icon)
    {
        // note the new selection
        _selection = icon;

        // deselect all other icons
        for (int ii = 0; ii < getComponentCount(); ii++) {
            SelectableIcon child = (SelectableIcon)getComponent(ii);
            if (child != icon) {
                child.setSelected(false);
            }
        }

        if (icon != null && _inspector != null) {
            _inspector.iconSelected(icon);
        }
    }

    protected Inspector _inspector;
    protected SelectableIcon _selection;
}
