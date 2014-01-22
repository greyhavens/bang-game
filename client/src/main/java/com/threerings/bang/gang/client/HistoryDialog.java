//
// $Id$

package com.threerings.bang.gang.client;

import java.text.SimpleDateFormat;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.ArrayUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.data.HistoryEntry;

/**
 * Allows the user to page through his gang's history.
 */
public class HistoryDialog extends BDecoratedWindow
    implements ActionListener, HideoutCodes
{
    public HistoryDialog (BangContext ctx, HideoutObject hideoutobj)
    {
        super(ctx.getStyleSheet(), ctx.xlate(HIDEOUT_MSGS, "t.history_dialog"));
        setModal(true);
        _ctx = ctx;
        _hideoutobj = hideoutobj;

        GroupLayout glay = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        _econt = new BContainer(glay);
        _econt.setStyleClass("history_view");
        add(_econt, GroupLayout.FIXED);

        _econt.add(new BLabel(_ctx.xlate(HIDEOUT_MSGS, "m.loading_history")));
        _econt.add(new Spacer(1, 1));

        BContainer acont = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
        BContainer fcont = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
        fcont.add(new BLabel(_ctx.xlate(HIDEOUT_MSGS, "m.history_filter")));
        _filter = new BComboBox();
        _filter.addItem(new BComboBox.Item(null, _ctx.xlate(HIDEOUT_MSGS, "m.filter_none")));
        for (String fstr : HideoutCodes.HISTORY_TYPES) {
            _filter.addItem(new BComboBox.Item(
                        fstr + "entry", _ctx.xlate(HIDEOUT_MSGS, fstr + "filter")));
        }
        _filter.selectItem(0);
        _filter.addListener(this);
        fcont.add(_filter);
        acont.add(fcont);
        ((GroupLayout)acont.getLayoutManager()).setGap(40);
        acont.add(_back = new BButton("", this, "back"));
        _back.setStyleClass("back_button");
        acont.add(_forward = new BButton("", this, "forward"));
        _forward.setStyleClass("fwd_button");
        add(acont, GroupLayout.FIXED);

        add(_status = new StatusLabel(ctx), GroupLayout.FIXED);

        BContainer bcont = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
        bcont.add(new BButton(ctx.xlate(HIDEOUT_MSGS, "m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);

        // hide the dialog until we hear back from the server
        setVisible(false);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("back")) {
            fetchEntries(_offset + HISTORY_PAGE_ENTRIES, (String)_filter.getSelectedValue());
        } else if (action.equals("forward")) {
            fetchEntries(Math.max(0, _offset - HISTORY_PAGE_ENTRIES),
                    (String)_filter.getSelectedValue());
        } else if (action.equals("selectionChanged")) {
            fetchEntries(0, (String)_filter.getSelectedValue());
        } else if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        pack();
        center();

        // fetch the first batch of entries
        fetchEntries(0, (String)_filter.getSelectedValue());
    }

    /**
     * Fetches the entries starting at the specified offset.
     */
    protected void fetchEntries (final int offset, final String filter)
    {
        final boolean fenable = _forward.isEnabled(), benable = _back.isEnabled();
        _forward.setEnabled(false);
        _back.setEnabled(false);
        _hideoutobj.service.getHistoryEntries(offset, filter, new HideoutService.ResultListener() {
            public void requestProcessed (Object result) {
                HistoryEntry[] entries = (HistoryEntry[])result;
                if (entries.length > HISTORY_PAGE_ENTRIES) {
                    // we received one more entry than necessary to let us know that we can go
                    // farther back
                    entries = ArrayUtil.splice(entries, 0, 1);
                    _back.setEnabled(true);
                } else {
                    _back.setEnabled(false);
                }
                updateEntries(entries, offset);
                _forward.setEnabled(_offset != 0);
                ensureVisible();
            }
            public void requestFailed (String cause) {
                _status.setStatus(HIDEOUT_MSGS, cause, true);
                _forward.setEnabled(fenable);
                _back.setEnabled(benable);
                ensureVisible();
            }
        });
    }

    /**
     * Shows the dialog if it's not already showing.
     */
    protected void ensureVisible ()
    {
        if (isVisible()) {
            return;
        }
        setVisible(true);
        _ctx.getBangClient().animatePopup(this, 500);
    }

    /**
     * Populates the UI with the retrieved history entries.
     */
    protected void updateEntries (HistoryEntry[] entries, int offset)
    {
        // if we received a partial page at the end, splice it into the next-to-last page and
        // adjust the offset accordingly
        if (entries.length < HISTORY_PAGE_ENTRIES && offset > 0) {
            HistoryEntry[] nentries = entries, oentries = _entries;
            entries = new HistoryEntry[HISTORY_PAGE_ENTRIES];
            System.arraycopy(nentries, 0, entries, 0, nentries.length);
            int overlap = HISTORY_PAGE_ENTRIES - nentries.length;
            System.arraycopy(oentries, 0, entries, nentries.length, overlap);
            offset -= overlap;
        }
        _entries = entries;
        _offset = offset;

        GroupLayout glay = GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.LEFT, GroupLayout.NONE);
        glay.setGap(10);
        glay.setOffAxisJustification(GroupLayout.TOP);

        _econt.removeAll();
        for (HistoryEntry entry : entries) {
            BContainer cont = new BContainer(glay);
            cont.add(new BLabel(DATE_FORMAT.format(entry.getRecordedDate()), "history_date"),
                GroupLayout.FIXED);
            BLabel desc = new BLabel(_ctx.xlate(HIDEOUT_MSGS, entry.description), "history_desc");
            cont.add(desc);
            _econt.add(cont);
            desc.getPreferredSize(325, -1); // hack to ensure label computes correct height
        }
        pack();
        center();
    }

    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
    protected MessageBundle _msgs;

    protected int _offset;
    protected HistoryEntry[] _entries;

    protected BContainer _econt;
    protected BButton _back, _forward;
    protected StatusLabel _status;
    protected BComboBox _filter;

    /** The date format to use for history entries. */
    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yy h:mm a");
}
