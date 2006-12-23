//
// $Id$

package com.threerings.bang.gang.client;

import java.text.SimpleDateFormat;

import java.util.Date;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

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
        super(ctx.getStyleSheet(), null);
        setModal(true);
        _ctx = ctx;
        _hideoutobj = hideoutobj;
        
        ((GroupLayout)getLayoutManager()).setOffAxisPolicy(GroupLayout.STRETCH);
        
        TableLayout tlay = new TableLayout(2, 2, 10);
        tlay.setHorizontalAlignment(TableLayout.STRETCH);
        tlay.setFixedColumn(0, true);
        _econt = new BContainer(tlay);
        _econt.setStyleClass("history_view");
        add(_econt);
        
        _econt.add(new BLabel(_ctx.xlate(HIDEOUT_MSGS, "m.loading_history")));
        _econt.add(new Spacer(1, 1));
        
        BContainer acont = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
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
    
        // fetch the first batch of entries    
        fetchEntries(0);
    }
    
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("back")) {
            fetchEntries(_offset + HISTORY_PAGE_ENTRIES);
        } else if (action.equals("forward")) {
            fetchEntries(Math.max(0, _offset - HISTORY_PAGE_ENTRIES));
        } else if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
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
        for (HistoryEntry entry : entries) {
            _econt.add(new BLabel(DATE_FORMAT.format(entry.getRecordedDate()), "history_entry"));
            _econt.add(new BLabel(_ctx.xlate(HIDEOUT_MSGS, entry.description), "history_entry"));
        }
    }
    
    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
    protected MessageBundle _msgs;
    
    protected int _offset;
    protected HistoryEntry[] _entries;
    
    protected BContainer _econt;
    protected BButton _back, _forward;
    protected StatusLabel _status;
    
    /** The date format to use for history entries. */
    protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yy h:mm a");
}
