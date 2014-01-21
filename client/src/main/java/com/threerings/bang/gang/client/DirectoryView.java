//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.MultiIconButton;
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
    public static BButton createArrowButton (
            BangContext ctx, ActionListener listener, String action)
    {
        String pref = "ui/icons/small_" + action + "_arrow";
        MultiIconButton button = new MultiIconButton(
            new ImageIcon(ctx.loadImage(pref + ".png")), listener, action);
        button.setIcon(new ImageIcon(ctx.loadImage(pref + "_disable.png")), BButton.DISABLED);
        button.setStyleClass("small_arrow_button");
        return button;
    }

    public DirectoryView (BangContext ctx, HideoutObject hideoutobj)
    {
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _hideoutobj = hideoutobj;

        setStyleClass("directory_view");

        // add the letters and arrows
        GroupLayout glay = GroupLayout.makeHoriz(GroupLayout.CENTER);
        glay.setGap(0);
        BContainer bcont = new BContainer(glay);
        bcont.add(_left = createArrowButton(_ctx, this, "left"));
        bcont.add(new Spacer(6, 1));
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
            _lbuttons[ii].setPreferredSize(new Dimension(14, -1));
            _lbuttons[ii].setStyleClass("directory_letter");
            _lbuttons[ii].addListener(this);
            bcont.add(_lbuttons[ii]);
        }
        bcont.add(new Spacer(6, 1));
        bcont.add(_right = createArrowButton(_ctx, this, "right"));
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
        if (src == _left) {
            showPage(_pidx - 1);
        } else if (src == _right) {
            showPage(_pidx + 1);
        } else {
            showPage(ListUtil.indexOf(_lbuttons, src));
        }
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
        String sstr = null;
        for (int ii = 0; ii < _lbuttons.length; ii++) {
            BToggleButton lbutton = _lbuttons[ii];
            String lstr = lbutton.getAction();
            if (ii == pidx) {
                lbutton.setSelected(true);
                lbutton.setText(StringUtil.toUSUpperCase(sstr = lstr));
            } else {
                lbutton.setSelected(false);
                lbutton.setText(lstr);
            }
        }

        _left.setEnabled(pidx > 0);
        _right.setEnabled(pidx < _lbuttons.length - 1);

        _pidx = pidx;
        _gcont.removeAll();
        for (GangEntry gang : _hideoutobj.gangs) {
            final Handle name = gang.name;
            String nstr = name.toString();
            if (StringUtil.toUSLowerCase(nstr).startsWith(sstr)) {
                _gcont.add(BangUI.createGangLabel(name, nstr, "directory_entry"));
            }
        }
    }

    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;

    protected BButton _left, _right;
    protected BToggleButton[] _lbuttons;
    protected BContainer _gcont;

    protected int _pidx;

    /** Refresh the page when entries are added to or removed from it. */
    protected SetAdapter<DSet.Entry> _dirlist = new SetAdapter<DSet.Entry>() {
        public void entryAdded (EntryAddedEvent<DSet.Entry> event) {
            if (isOnCurrentPage(event.getEntry().getKey())) {
                showPage(_pidx);
            }
        }
        public void entryRemoved (EntryRemovedEvent<DSet.Entry> event) {
            if (isOnCurrentPage(event.getKey())) {
                showPage(_pidx);
            }
        }
        protected boolean isOnCurrentPage (Comparable<?> key) {
            String lstr = _lbuttons[_pidx].getAction();
            return StringUtil.toUSLowerCase(key.toString()).startsWith(lstr);
        }
    };
}
