//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.gang.data.GangEntry;
import com.threerings.bang.gang.data.HideoutObject;

/**
 * Allows the user to browse the list of active gangs by letter.
 */
public class DirectoryView extends BContainer
    implements ActionListener
{
    public DirectoryView (BangContext ctx, HideoutObject hideoutobj)
    {
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _hideoutobj = hideoutobj;

        setStyleClass("directory_view");

        // add the letter buttons
        GroupLayout glay = GroupLayout.makeHoriz(GroupLayout.CENTER);
        glay.setGap(0);
        BContainer bcont = new BContainer(glay);
        char[] letters = NameFactory.getValidator().getValidLetters();
        _lbuttons = new BToggleButton[letters.length];
        for (int ii = 0; ii < letters.length; ii++) {
            String cstr = Character.toString(letters[ii]);
            _lbuttons[ii] = new BToggleButton(cstr, cstr) {
                protected void fireAction (long when, int modifiers) {
                    if (!_selected) { // only selection, no deselection
                        super.fireAction(when, modifiers);
                    }
                }
            };
            _lbuttons[ii].setStyleClass("directory_letter");
            _lbuttons[ii].addListener(this);
            bcont.add(_lbuttons[ii]);
        }
        add(bcont, GroupLayout.FIXED);

        // add the group entry container
        glay = GroupLayout.makeVert(GroupLayout.TOP);
        glay.setOffAxisJustification(GroupLayout.LEFT);
        BScrollPane gpane = new BScrollPane(_gcont = new BContainer(glay));
        gpane.setStyleClass("directory_pane");
        add(gpane);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        Object src = event.getSource();
        int pidx = -1;
        for (int ii = 0; ii < _lbuttons.length; ii++) {
            BToggleButton button = _lbuttons[ii];
            if (button == src) {
                pidx = ii;
            } else if (button != src && button.isSelected()) {
                button.setSelected(false);
                button.setText(button.getText().toLowerCase());
            }
        }
        showPage(pidx);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        showPage(_pidx);
        _hideoutobj.addListener(_dirlist);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _hideoutobj.removeListener(_dirlist);
    }

    protected void showPage (int pidx)
    {
        BToggleButton button = _lbuttons[pidx];
        button.setSelected(true);
        String lstr = button.getAction();
        button.setText(lstr.toUpperCase());

        _pidx = pidx;
        _gcont.removeAll();
        for (GangEntry gang : _hideoutobj.gangs) {
            final Handle name = gang.name;
            String nstr = name.toString();
            if (nstr.toLowerCase().startsWith(lstr)) {
                _gcont.add(BangUI.createGangLabel(name, nstr, "directory_entry"));
            }
        }
    }

    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;

    protected BToggleButton[] _lbuttons;
    protected BContainer _gcont;

    protected int _pidx;

    /** Refresh the page when entries are added to or removed from it. */
    protected SetAdapter _dirlist = new SetAdapter() {
        public void entryAdded (EntryAddedEvent event) {
            if (isOnCurrentPage(event.getEntry().getKey())) {
                showPage(_pidx);
            }
        }
        public void entryRemoved (EntryRemovedEvent event) {
            if (isOnCurrentPage(event.getKey())) {
                showPage(_pidx);
            }
        }
        protected boolean isOnCurrentPage (Comparable key) {
            String lstr = _lbuttons[_pidx].getAction();
            return key.toString().toLowerCase().startsWith(lstr);
        }
    };
}
