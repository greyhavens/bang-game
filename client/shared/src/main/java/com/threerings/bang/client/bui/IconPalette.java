//
// $Id$

package com.threerings.bang.client.bui;

import java.util.ArrayList;

import com.badlogic.gdx.Input.Keys;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.bang.client.BangUI;

/**
 * Displays a palette of icons that can be selected by the user.
 */
public class IconPalette extends BContainer
{
    /** Used to inform an external inspector that a particular icon has been selected. */
    public interface Inspector
    {
        /** Called to indicate that an icon has been selected or deselected. */
        public void iconUpdated (SelectableIcon icon, boolean selected);
    }

    /**
     * Creates an icon palette with the supplied (optional) inspector. Icons must be added via
     * {@link #addIcon} and must extend {@link SelectableIcon}.
     *
     * @param columns the number of columns of icons to display.
     * @param rows the number of rows of icons to display.
     * @param isize the dimensions of the icons we will contain.
     * @param selectable the number of simultaneously selectable icons (may be zero).
     */
    public IconPalette (Inspector inspector, int columns, int rows, Dimension isize, int selectable)
    {
        super(new BorderLayout(0, 0));

        _inspector = inspector;
        _selectable = selectable;

        // we need an extra container around our icon container so that we can paint a border
        _iicont = GroupLayout.makeVBox(GroupLayout.CENTER);
        add(_iicont, BorderLayout.NORTH);
        init(columns, rows, isize);

        GroupLayout hlay = GroupLayout.makeHoriz(GroupLayout.RIGHT);
        hlay.setGap(35);
        _bcont = new BContainer(hlay);
        _bcont.setStyleClass("palette_buttons");
        _bcont.add(_back = new BButton("", _listener, "back"));
        _back.setStyleClass("back_button");
        _bcont.add(_forward = new BButton("", _listener, "forward"));
        _forward.setStyleClass("fwd_button");
        add(_bcont, BorderLayout.SOUTH);
    }

    public void init (int columns, int rows, Dimension isize)
    {
        _rows = rows;
        _cols = columns;
        if (_icont == null) {
            _iicont.add(_icont = new BContainer());
        }
        _icont.setLayoutManager(new TableLayout(columns, 0, 0));
        _icont.setPreferredSize(new Dimension(isize.width * columns, isize.height * rows));
    }

    /**
     * Instructs the palette to display the specified page.
     */
    public void displayPage (int page)
    {
        if (isAdded()) {
            displayPage(page, true, false);
        } else {
            _page = page;
        }
    }

    /**
     * Configures whether or not the last selected icon can be deselected.
     */
    public void setAllowsEmptySelection (boolean allowsEmptySelection)
    {
        _allowsEmpty = allowsEmptySelection;
    }

    /**
     * Returns the number of icons that may be simultaneously selected (which may be zero).
     */
    public int getSelectable ()
    {
        return _selectable;
    }

    /**
     * Configures the number of simultaneous selections allowed by this icon palette.
     */
    public void setSelectable (int selectable)
    {
        _selectable = selectable;

        // reduce the current selection to the new selectable count
        while (_selections.size() > _selectable) {
            _selections.remove(_selections.size()-1).setSelected(false);
        }
    }

    /**
     * Enables or disables the painting of a background image behind the icons in this palette. The
     * default is not to paint a background.
     */
    public void setPaintBackground (boolean paintbg)
    {
        _icont.setStyleClass(paintbg ? "palette_background" : null);
    }

    /**
     * Enables or disables the painting of a fancy border around the icons (but not enclosing the
     * forward/back buttons). The default is not to paint a border.
     */
    public void setPaintBorder (boolean paintborder)
    {
        _iicont.setStyleClass(paintborder ? "palette_border" : null);
    }

    /**
     * Configures whether or not the navigation buttons are show. The default is to show the
     * navigation buttons.
     */
    public void setShowNavigation (boolean shownav)
    {
        if (_bcont.getParent() == null && shownav) {
            add(_bcont, BorderLayout.CENTER);
        } else if (_bcont.getParent() != null && !shownav) {
            remove(_bcont);
        }
    }

    /**
     * Returns a reference to the container holding the navigation buttons, so that they
     * can be placed elsewhere.
     */
    public BContainer getNavigationContainer ()
    {
        return _bcont;
    }

    /**
     * Adds an icon to this palette. Do not add icons directly with {@link #add}.
     */
    public void addIcon (SelectableIcon icon)
    {
        addIcon(_icons.size(), icon);
    }

    /**
     * Adds an icon to this palette. Do not add icons directly with {@link #add}.
     *
     * @param idx the index at which to add the icon
     */
    public void addIcon (int idx, SelectableIcon icon)
    {
        _icons.add(idx, icon);
        icon.setPalette(this);

        if (isAdded()) {
            // update the current page of icons if necessary
            int ipage = idx/(_rows*_cols);
            if (ipage <= _page) {
                displayPage(_page, true, false);
            } else {
                _forward.setEnabled(true);
            }
        }
    }

    /**
     * Removes an icon from the palette (and from the display if it is showing).
     */
    public void removeIcon (SelectableIcon icon)
    {
        int idx = _icons.indexOf(icon);
        icon.setSelected(false);
        icon.setPalette(null);
        _icons.remove(idx);

        if (isAdded()) {
            // update the current page of icons if necessary
            int psize = _rows * _cols,
                ipage = idx/psize;
            if (ipage <= _page) {
                displayPage(_page, true, false);
            } else {
                _forward.setEnabled(_icons.size() > (_page+1)*psize);
            }
        }
    }

    /**
     * Returns the number of icons in the palette.
     */
    public int getIconCount ()
    {
        return _icons.size();
    }

    /**
     * Returns the icon at the specified index.
     */
    public SelectableIcon getIcon (int idx)
    {
        return _icons.get(idx);
    }

    /**
     * Returns an array containing the icons displayed by this palette.
     */
    public SelectableIcon[] getIcons ()
    {
        return _icons.toArray(new SelectableIcon[_icons.size()]);
    }

    /**
     * Selects the first icon if there is at least one icon in the palette.
     */
    public void selectFirstIcon ()
    {
        if (_icons.size() > 0) {
            _icons.get(0).setSelected(true);
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
     * Returns the <code>index</code>th selected icon or null if no icon is selected at that index.
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

    /**
     * Called by a {@link SelectableIcon} when it changes state.
     */
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        if (selected && !_selections.contains(icon)) {
            iconSelected(icon);
        } else if (!selected && _selections.contains(icon)) {
            iconDeselected(icon);
        }
    }

    // documentation inherited
    public boolean acceptsFocus ()
    {
        return isEnabled();
    }

    // documentation inherited
    public boolean dispatchEvent (BEvent event)
    {
        if (event instanceof KeyEvent) {
            KeyEvent kev = (KeyEvent)event;
            if (kev.getType() == KeyEvent.KEY_PRESSED) {
                switch (kev.getKeyCode()) {
                case Keys.LEFT:
                    moveSelection(-1);
                    break;
                case Keys.RIGHT:
                    moveSelection(1);
                    break;
                case Keys.UP:
                    moveSelection(-_cols);
                    break;
                case Keys.DOWN:
                    moveSelection(_cols);
                    break;
                default:
                    return super.dispatchEvent(event);
                }
                return true;
            }
        }

        return super.dispatchEvent(event);
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
            displayPage(_page, true, true);
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

        // if we have only one row of icons, ignore requests to move up and down a whole row
        if (_icons.size() <= _cols && Math.abs(delta) != 1) {
            return;
        }

        // if we have no current selection, just select the first icon
        if (_selections.size() == 0) {
            displayPage(0, false, false);
            _icons.get(0).setSelected(true);

        } else {
            int selidx = _icons.indexOf(_selections.get(0));
            selidx = (selidx + delta + _icons.size()) % _icons.size();
            displayPage(selidx / (_rows*_cols), false, false);
            // skip over disabled icons
            int adjust = (delta > 0) ? 1 : -1;
            for (int ii = 0; !_icons.get(selidx).isEnabled() &&
                     ii < _icons.size()-1; ii++) {
                selidx = (selidx + adjust + _icons.size()) % _icons.size();
            }
            if (_icons.get(selidx).isEnabled()) {
                _icons.get(selidx).setSelected(true);
            }
        }

        // play a sound as this caused an icon to become selected as a result of user input
        BangUI.play(BangUI.FeedbackSound.ITEM_SELECTED);
    }

    protected void displayPage (int page, boolean force, boolean focus)
    {
        if (_page != page || force) {
            _icont.removeAll();
            int start = page * _rows * _cols;
            int limit = Math.min(_icons.size(), start + _rows * _cols);
            for (int ii = start; ii < limit; ii++) {
                _icont.add(_icons.get(ii));
            }
            _page = page;
            _forward.setEnabled(limit < _icons.size());
            _back.setEnabled(start > 0);
        }

        // rerequest focus when the user clicks a forward or back button
        if (focus) {
            requestFocus();
        }
    }

    protected void clearDisplay ()
    {
        clearSelections();
        _icont.removeAll();
        _back.setEnabled(false);
        _forward.setEnabled(false);
        _page = 0;
    }

    protected void iconSelected (SelectableIcon icon)
    {
        // this happens when we're refusing an empty selection
        if (_selections.contains(icon)) {
            return;
        }

        // add the newly selected icon to the list of selections
        _selections.add(icon);

        // and pop the first one off the list if necessary
        while (_selections.size() > _selectable) {
            _selections.get(0).setSelected(false);
        }

        // inform our inspector that this icon was selected
        if (_selectable > 0 && icon != null && _inspector != null) {
            _inspector.iconUpdated(icon, true);
        }
    }

    protected void iconDeselected (SelectableIcon icon)
    {
        // refuse to allow the last icon to be deselected if we don't allow an empty selection
        if (!_allowsEmpty && _selections.size() == 1 && _selections.get(0) == icon) {
            icon.setSelected(true);
            return;
        }

        // the icon was deselected, remove it from the selections list
        _selections.remove(icon);

        // inform our inspector that an icon was deselected
        if (_inspector != null) {
            _inspector.iconUpdated(icon, false);
        }
    }

    protected ActionListener _listener = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            if (event.getAction().equals("forward")) {
                displayPage(_page+1, false, true);
            } else if (event.getAction().equals("back")) {
                displayPage(_page-1, false, true);
            }
        }
    };

    protected ArrayList<SelectableIcon> _icons = new ArrayList<SelectableIcon>();
    protected int _rows, _cols, _page;

    protected BContainer _iicont, _icont, _bcont;
    protected Inspector _inspector;
    protected BButton _forward, _back;

    protected boolean _allowsEmpty = true;
    protected int _selectable;
    protected ArrayList<SelectableIcon> _selections = new ArrayList<SelectableIcon>();
}
