//
// $Id$

package com.threerings.bang.client.bui;

import java.util.ArrayList;

import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BContainer;
import com.jmex.bui.border.CompoundBorder;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.border.LineBorder;
import com.jmex.bui.layout.TableLayout;

import static com.threerings.bang.Log.log;

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
     *
     * @param columns the number of columns of icons to display.
     * @param selectable the number of simultaneously selectable icons (must be
     * at least one).
     */
    public IconPalette (Inspector inspector, int columns, int selectable)
    {
        super(new TableLayout(columns, 5, 5));
        setBorder(new CompoundBorder(new LineBorder(ColorRGBA.black),
                                     new EmptyBorder(5, 5, 5, 5)));
        _inspector = inspector;
        _selectable = selectable;
    }

    /**
     * Returns the selected icon or null if none is selected.
     */
    public SelectableIcon getSelectedIcon ()
    {
        return getSelectedIcon(0);
    }

    /**
     * Returns the <code>index</code>th selected icon or null if no icon is
     * selected at that index.
     */
    public SelectableIcon getSelectedIcon (int index)
    {
        return _selections.size() > index ? _selections.get(index) : null;
    }

    /**
     * Clears all selected icons.
     */
    public void clearSelections ()
    {
        while (_selections.size() > 0) {
            _selections.remove(0).setSelected(false);
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        clearSelections();
    }

    protected void iconUpdated (SelectableIcon icon, boolean selected)
    {
        if (selected && !_selections.contains(icon)) {
            // add the newly selected icon to the list of selections
            _selections.add(icon);

            // and pop the first one off the list if necessary
            while (_selections.size() > _selectable) {
                _selections.remove(0).setSelected(false);
            }

            // inform our inspector that this icon was selected
            if (icon != null && _inspector != null) {
                _inspector.iconSelected(icon);
            }

        } else if (!selected && _selections.contains(icon)) {
            // the icon was deselected, remove it from the selections list
            _selections.remove(icon);
        }
    }

    protected Inspector _inspector;
    protected int _selectable;
    protected ArrayList<SelectableIcon> _selections =
        new ArrayList<SelectableIcon>();
}
