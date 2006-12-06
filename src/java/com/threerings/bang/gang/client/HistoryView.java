//
// $Id$

package com.threerings.bang.gang.client;

import java.text.SimpleDateFormat;

import java.util.Date;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.data.HistoryEntry;

/**
 * Displays the gang's historical entries, fetching them from the server one page at a time.
 */
public class HistoryView extends BContainer
    implements ActionListener, HideoutCodes
{
    public HistoryView (BangContext ctx, StatusLabel status, HideoutObject hideoutobj)
    {
        super(new BorderLayout());
        _ctx = ctx;
        _status = status;
        _hideoutobj = hideoutobj;
        
        GroupLayout vlay = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        _econt = new BContainer(vlay);
        add(_econt, BorderLayout.CENTER);
        
        _econt.add(new BLabel(_ctx.xlate(HIDEOUT_MSGS, "m.loading_history")));
                
        GroupLayout hlay = GroupLayout.makeHoriz(GroupLayout.RIGHT);
        hlay.setGap(35);
        BContainer bcont = new BContainer(hlay);
        bcont.setStyleClass("palette_buttons");
        bcont.add(_back = new BButton("", this, "back"));
        _back.setStyleClass("back_button");
        bcont.add(_forward = new BButton("", this, "forward"));
        _forward.setStyleClass("fwd_button");
        _forward.setEnabled(false);
        add(bcont, BorderLayout.SOUTH);
    }
 
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        Object src = event.getSource();
        if (src == _forward) {
            fetchEntries(Math.max(0, _offset - HISTORY_PAGE_ENTRIES));
        } else if (src == _back) {
            fetchEntries(_offset + HISTORY_PAGE_ENTRIES);
        }
    }
    
    @Override // documentation inherited
    protected void wasAdded ()
    {
        // if we are on the last page, fetch the most recent entries
        super.wasAdded();
        if (_offset == 0) {
            fetchEntries(0);
        }
    }
    
    /**
     * Fetches the entries starting at the specified offset.
     */
    protected void fetchEntries (final int offset)
    {
        final boolean fenable = _forward.isEnabled(), benable = _back.isEnabled();
        _forward.setEnabled(false);
        _back.setEnabled(false);
        _hideoutobj.service.getHistoryEntries(_ctx.getClient(), offset,
            new HideoutService.ResultListener() {
            public void requestProcessed (Object result) {
                HistoryEntry[] entries = (HistoryEntry[])result;
                updateEntries(entries, offset);
                _forward.setEnabled(_offset != 0);
                _back.setEnabled(entries.length == HISTORY_PAGE_ENTRIES);
            }
            public void requestFailed (String cause) {
                _status.setStatus(HIDEOUT_MSGS, cause, true);
                _forward.setEnabled(fenable);
                _back.setEnabled(benable);
            }
        });
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
        
        _econt.removeAll();
        GroupLayout hlay = GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.EQUALIZE);
        for (HistoryEntry entry : entries) {
            BContainer line = new BContainer(hlay);
            line.add(new BLabel(DATE_FORMAT.format(entry.getRecordedDate())), GroupLayout.FIXED);
            line.add(new BLabel(_ctx.xlate(HIDEOUT_MSGS, entry.description)));
            _econt.add(line);
        }
    }
    
    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
    
    protected int _offset;
    protected HistoryEntry[] _entries;
    
    protected BContainer _econt;
    protected BButton _forward, _back;
    protected StatusLabel _status;
    
    /** The date format to use for history entries. */
    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yy h:mm a");
}
