//
// $Id$

package com.threerings.bang.client.bui;

import java.util.ArrayList;

import com.jme.input.KeyInput;
import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

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

        /** Called when there is no longer a selection. */
        public void selectionCleared ();
    }

    /**
     * Creates an icon palette with the supplied (optional) inspector. Icons
     * may be added via {@link #add} but they must derive from {@link
     * SelectableIcon}.
     *
     * @param columns the number of columns of icons to display.
     * @param rows the number of rows of icons to display.
     * @param size the dimensions of the icons we will contain.
     * @param selectable the number of simultaneously selectable icons (must be
     * at least one).
     */
    public IconPalette (Inspector inspector, int columns, int rows,
                        Dimension size, int selectable)
    {
        super(new BorderLayout(0, 0));
        _rows = rows;
        _cols = columns;
        _inspector = inspector;
        _selectable = selectable;

        add(_icont = new BContainer(new TableLayout(columns, 0, 0)),
            BorderLayout.CENTER);
        _icont.setPreferredSize(new Dimension(size.width * columns,
                                              size.height * rows));

        GroupLayout hlay = GroupLayout.makeHoriz(GroupLayout.RIGHT);
        hlay.setGap(50);
        BContainer buttons = new BContainer(hlay);
        buttons.setStyleClass("palette_buttons");
        buttons.add(_back = new BButton("", _listener, "back"));
        _back.setStyleClass("back_button");
        buttons.add(_forward = new BButton("", _listener, "forward"));
        _forward.setStyleClass("fwd_button");
        add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Adds an icon to this palette. Use this method instead of {@link #add} so
     * that the palette can properly page through the icons when there are
     * multiple pages.
     */
    public void addIcon (SelectableIcon icon)
    {
        _icons.add(icon);

        if (isAdded()) {
            // potentially add this icon to the display if we're on its page
            int ipage = (_icons.size()-1)/(_rows*_cols);
            if (_page == ipage) {
                _icont.add(icon);
            } else {
                _forward.setEnabled(true);
            }
        }
    }

    /**
     * Removes an icon from the palette (and from the display if it is
     * showing).
     */
    public void removeIcon (SelectableIcon icon)
    {
        _icons.remove(icon);
        if (icon.isAdded()) {
            _icont.remove(icon);
        }
    }

    /**
     * Configures the inspector for this palette.
     */
    public void setInspector (Inspector inspector)
    {
        _inspector = inspector;
        requestFocus();
    }

    /**
     * Clears all icons from this palette.
     */
    public void clear ()
    {
        clearDisplay();
        _icons.clear();
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
        int sels = _selections.size();
        while (_selections.size() > 0) {
            _selections.remove(0).setSelected(false);
        }
        if (sels > 0 && _inspector != null && isAdded()) {
            _inspector.selectionCleared();
        }
    }

    // documentation inherited
    public boolean acceptsFocus ()
    {
        return isEnabled();
    }

    // documentation inherited
    public void dispatchEvent (BEvent event)
    {
        if (event instanceof KeyEvent) {
            KeyEvent kev = (KeyEvent)event;
            if (kev.getType() == KeyEvent.KEY_PRESSED) {
                switch (kev.getKeyCode()) {
                case KeyInput.KEY_LEFT:
                    moveSelection(-1);
                    break;
                case KeyInput.KEY_RIGHT:
                    moveSelection(1);
                    break;
                case KeyInput.KEY_UP:
                    moveSelection(-_cols);
                    break;
                case KeyInput.KEY_DOWN:
                    moveSelection(_cols);
                    break;
                default:
                    super.dispatchEvent(event);
                    break;
                }
            }

        } else {
            super.dispatchEvent(event);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // nothing to do if we have no icons
        if (_icons.size() == 0) {
            _forward.setEnabled(false);
            _back.setEnabled(false);
        } else {
            // add the icons for the page that's showing
            displayPage(_page, true);
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        clearDisplay();
    }

    protected void moveSelection (int delta)
    {
        // no icons means nothing do to
        if (_icons.size() == 0) {
            return;
        }

        // if we have only one row of icons, ignore requests to move up and
        // down a whole row
        if (_icons.size() <= _cols && Math.abs(delta) != 1) {
            return;
        }

        // if we have no current selection, just select the first icon
        if (_selections.size() == 0) {
            displayPage(0, false);
            _icons.get(0).setSelected(true);
        }

        int selidx = _icons.indexOf(_selections.get(0));
        selidx = (selidx + delta + _icons.size()) % _icons.size();
        displayPage(selidx / (_rows*_cols), false);
        _icons.get(selidx).setSelected(true);
    }

    protected void displayPage (int page, boolean force)
    {
        if (_page != page || force) {
            clearSelections();
            _icont.removeAll();
            int start = page * _rows;
            int limit = Math.min(_icons.size(), start + _rows * _cols);
            for (int ii = start; ii < limit; ii++) {
                _icont.add(_icons.get(ii));
            }
            _page = page;
            _forward.setEnabled(limit < _icons.size());
            _back.setEnabled(start > 0);
        }

        // rerequest focus as the user just clicked a forward or back button
        requestFocus();
    }

    protected void clearDisplay ()
    {
        clearSelections();
        _icont.removeAll();
        _back.setEnabled(false);
        _forward.setEnabled(false);
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

            // if this was the last selected icon, inform our inspector that
            // the seleciton was cleared
            if (_inspector != null && _selections.size() == 0) {
                _inspector.selectionCleared();
            }
        }
    }

    protected ActionListener _listener = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            if (event.getAction().equals("forward")) {
                displayPage(_page+1, false);
            } else if (event.getAction().equals("back")) {
                displayPage(_page-1, false);
            }
        }
    };

    protected ArrayList<SelectableIcon> _icons =
        new ArrayList<SelectableIcon>();
    protected int _rows, _cols, _page;

    protected BContainer _icont;
    protected Inspector _inspector;
    protected BButton _forward, _back;

    protected int _selectable;
    protected ArrayList<SelectableIcon> _selections =
        new ArrayList<SelectableIcon>();
}
